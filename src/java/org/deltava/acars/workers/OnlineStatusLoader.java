// Copyright 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.io.*;
import java.util.*;
import java.time.Instant;
import java.sql.Connection;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import org.deltava.beans.OnlineNetwork;
import org.deltava.beans.servinfo.NetworkInfo;

import org.deltava.dao.*;
import org.deltava.dao.file.*;
import org.deltava.dao.http.GetURL;

import org.deltava.util.TaskTimer;
import org.deltava.util.cache.*;
import org.deltava.util.system.SystemData;

import org.gvagroup.ipc.WorkerState;
import org.gvagroup.jdbc.ConnectionPool;

/**
 * An ACARS worker thread to load online network status.
 * @author Luke
 * @version 9.0
 * @since 9.0
 */

public class OnlineStatusLoader extends Worker {

	private final Cache<NetworkInfo> _cache = CacheManager.get(NetworkInfo.class, "ServInfoData");
	private final Collection<Loader> LOADERS = List.of(new VATSIMLoader(), new PilotEdgeLoader(), new IVAOLoader());

	private ConnectionPool _jdbcPool;

	private static final int SLEEP_INTERVAL = 30;

	private abstract class Loader implements Runnable {
		protected Instant _lastUpdate = Instant.ofEpochMilli(0);
		protected Instant _lastRun = Instant.ofEpochMilli(0);
		final OnlineNetwork _network;
		final int _updateInterval;
		protected boolean _isUpdated;

		protected Loader(OnlineNetwork net, int updateInterval) {
			_network = net;
			_updateInterval = Math.max(5, updateInterval);
		}

		protected void update(NetworkInfo inf) {
			_lastUpdate = inf.getValidDate();
			_cache.add(inf);
			_isUpdated = true;
		}
		
		@Override
		public void run() {
			_lastRun = Instant.now();
			_isUpdated = false;
		}
	}

	private class PilotEdgeLoader extends Loader {
		PilotEdgeLoader() {
			super(OnlineNetwork.PILOTEDGE, 30);
		}

		@Override
		public void run() {
			super.run();
			try {
				GetURL urldao = new GetURL(SystemData.get("online.pilotedge.status_url"), SystemData.get("online.pilotedge.local.info"));
				urldao.setConnectTimeout(5000);
				urldao.setReadTimeout(15000);
				File f = urldao.download();
				try (InputStream is = new BufferedInputStream(new FileInputStream(f), 32768)) {
					GetServInfo sidao = new GetServInfo(is, _network);
					NetworkInfo in = sidao.getInfo();
					if ((in != null) && in.getValidDate().isAfter(_lastUpdate)) {
						update(in);
						f.setLastModified(in.getValidDate().toEpochMilli());
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	private class VATSIMLoader extends Loader {
		VATSIMLoader() {
			super(OnlineNetwork.VATSIM, 30);
		}

		@Override
		public void run() {
			super.run();
			try {
				GetURL urldao = new GetURL(SystemData.get("online.vatsim.status_url"), SystemData.get("online.vatsim.local.info"));
				urldao.setConnectTimeout(5000);
				urldao.setReadTimeout(15000);
				File f = urldao.download();
				try (InputStream is = new BufferedInputStream(new FileInputStream(f), 131072)) {
					GetVATSIMInfo sidao = new GetVATSIMInfo(is);
					NetworkInfo in = sidao.getInfo();
					if ((in != null) && in.getValidDate().isAfter(_lastUpdate)) {
						update(in);
						f.setLastModified(in.getValidDate().toEpochMilli());
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	private class IVAOLoader extends Loader {
		IVAOLoader() {
			super(OnlineNetwork.IVAO, 60);
		}

		@Override
		public void run() {
			super.run();
			String url = null;
			try {
				GetURL urldao = new GetURL(SystemData.get("online.ivao.status_url"), SystemData.get("online.ivao.local.status"));
				urldao.setConnectTimeout(3500);
				urldao.setReadTimeout(4500);
				Properties p = new Properties();
				File f = urldao.download();
				try (InputStream is = new FileInputStream(f)) {
					p.load(is);
				}

				url = p.getProperty("url0");
				urldao = new GetURL(url, SystemData.get("online.ivao.local.info"));
				urldao.setConnectTimeout(3500);
				urldao.setReadTimeout(25000);
				f = urldao.download();
				try (InputStream is = new BufferedInputStream(new FileInputStream(f), 65536)) {
					GetServInfo sidao = new GetServInfo(is, _network);
					NetworkInfo in = sidao.getInfo();
					if ((in != null) && in.getValidDate().isAfter(_lastUpdate)) {
						update(in);
						f.setLastModified(in.getValidDate().toEpochMilli());
					}
				}
			} catch (Exception e) {
				boolean isTimeout = e.getCause() instanceof java.net.SocketTimeoutException;
				if (isTimeout)
					log.warn("Timeout loading " + url);
				else
					log.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * Initializes the worker.
	 */
	public OnlineStatusLoader() {
		super("ServInfo Loader", 45, OnlineStatusLoader.class);
	}

	@Override
	public void open() {
		super.open();
		_jdbcPool = (ConnectionPool) SystemData.getObject(SystemData.JDBC_POOL);
		
		// Init the update dates
		for (Loader l : LOADERS) {
			NetworkInfo inf = _cache.get(l._network);
			if (inf != null) {
				l._lastUpdate = inf.getValidDate();
				l._lastRun = inf.getValidDate().minusSeconds(10);
			}
		}
	}

	@Override
	public void run() {
		log.info("Started");
		_status.setStatus(WorkerState.RUNNING);

		ForkJoinPool pool = new ForkJoinPool(4);
		while (!Thread.currentThread().isInterrupted()) {
			_status.setMessage("Downloading network data");
			_status.execute();

			try {
				long now = System.currentTimeMillis();
				long sleepTime = (SLEEP_INTERVAL * 1000);
				List<Loader> tasks = LOADERS.stream().filter(l -> ((now - l._lastRun.toEpochMilli()) > l._updateInterval)).collect(Collectors.toList());
				if (!tasks.isEmpty()) {
					tasks.forEach(pool::submit); TaskTimer tt = new TaskTimer();
					pool.awaitQuiescence((SLEEP_INTERVAL - 2), TimeUnit.SECONDS);
					long ms = tt.stop(); sleepTime -= ms;
					_status.complete();
					if (!pool.isQuiescent()) {
						log.warn("Pool still active after " + ms + "ms");
						sleepTime += (SLEEP_INTERVAL * 1000);
					}

					// Record the update times for outage tracking
					Connection c = null; tasks.removeIf(l -> !l._isUpdated);
					if (!tasks.isEmpty()) {
						_status.setMessage("Updating data validity");
						
						try {
							c = _jdbcPool.getConnection();
							SetOnlineTrack twdao = new SetOnlineTrack(c);
							for (Loader l : tasks)
								twdao.writePull(l._network, l._lastUpdate);
						} catch (DAOException de) {
							log.error("Error writing update times - " + de.getMessage(), de);
						} finally {
							_jdbcPool.release(c);
						}
					}
				}

				_status.setMessage("Idle");
				Thread.sleep(sleepTime);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}
}