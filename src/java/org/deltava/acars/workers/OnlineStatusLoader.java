// Copyright 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.io.*;
import java.util.*;
import java.time.Instant;
import java.sql.Connection;
import java.util.concurrent.*;

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
 * An ACARS worker thread to load online network status
 * @author Luke
 * @version 9.0
 * @since 9.0
 */

public class OnlineStatusLoader extends Worker {
	
	private final Cache<NetworkInfo> _cache = CacheManager.get(NetworkInfo.class, "ServInfoData");
	
	private ConnectionPool _jdbcPool;
	
	private abstract class Loader implements Runnable {
		protected Instant _lastUpdate = Instant.ofEpochMilli(0);
		final OnlineNetwork _network;
		protected boolean _isUpdated;
		
		protected Loader(OnlineNetwork net) {
			_network = net;
		}
		
		protected void update(NetworkInfo inf) {
			_lastUpdate = inf.getValidDate();
			_cache.add(inf);
			_isUpdated = true;
		}
	}
	
	private class PilotEdgeLoader extends Loader {
		PilotEdgeLoader() {
			super(OnlineNetwork.PILOTEDGE);
		}
		
		@Override
		public void run() {
			_isUpdated = false;
			try {
				GetURL urldao = new GetURL(SystemData.get("online.pilotedge.status_url"), SystemData.get("online.pilotedge.local.info"));
				urldao.setConnectTimeout(5000);
				urldao.setReadTimeout(15000);
				File f = urldao.download();
				try (InputStream is = new BufferedInputStream(new FileInputStream(f))) {
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
			super(OnlineNetwork.VATSIM);
		}

		@Override
		public void run() {
			_isUpdated = false;
			try {
				GetURL urldao = new GetURL(SystemData.get("online.vatsim.status_url"), SystemData.get("online.vatsim.local.info"));
				urldao.setConnectTimeout(5000);
				urldao.setReadTimeout(15000);
				File f = urldao.download();
				try (InputStream is = new BufferedInputStream(new FileInputStream(f))) {
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
			super(OnlineNetwork.IVAO);
		}

		@Override
		public void run() {
			_isUpdated = false;
			try {
				GetURL urldao = new GetURL(SystemData.get("online.ivao.status_url"), SystemData.get("online.ivao.local.status"));
				urldao.setConnectTimeout(5000);
				urldao.setReadTimeout(22500);
				Properties p = new Properties();
				File f = urldao.download();
				try (InputStream is = new FileInputStream(f)) {
					p.load(is);
				}

				log.debug("Loading IVAO data at " + p.getProperty("url0"));
				urldao = new GetURL(p.getProperty("url0"), SystemData.get("online.ivao.local.info"));
				urldao.setConnectTimeout(5000);
				urldao.setReadTimeout(15000);
				f = urldao.download();
				try (InputStream is = new BufferedInputStream(new FileInputStream(f))) {
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
	}

	@Override
	public void run() {
		log.info("Started");
		_status.setStatus(WorkerState.RUNNING);
		
		Collection<Loader> tasks = List.of(new VATSIMLoader(), new PilotEdgeLoader(), new IVAOLoader());
		ForkJoinPool pool = new ForkJoinPool(4); 
		int sleepInterval = SystemData.getInt("online.refresh_interval", 60);
		while (!Thread.currentThread().isInterrupted()) {
			_status.setMessage("Downloading network data");
			_status.execute();
			tasks.forEach(pool::submit);
			
			TaskTimer tt = new TaskTimer();
			try {
				pool.awaitQuiescence((sleepInterval - 2), TimeUnit.SECONDS);
				long ms = tt.stop(); long sleepTime = (sleepInterval * 1000) - ms;
				_status.complete();
				if (!pool.isQuiescent()) {
					log.warn("Pool still active after " + ms + "ms");
					sleepTime += (sleepInterval * 1000);
				}
				
				// Record the update times for outage tracking
				Connection c = null;
				_status.setMessage("Updating update times");
				try {
					c = _jdbcPool.getConnection();
					SetOnlineTrack twdao = new SetOnlineTrack(c);
					for (Loader l : tasks) 
					{
						if (l._isUpdated)
							twdao.writePull(l._network, l._lastUpdate);
					}
				} catch (DAOException de) {
					log.error("Error writing update times - " + de.getMessage(), de);
				} finally {
					_jdbcPool.release(c);
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