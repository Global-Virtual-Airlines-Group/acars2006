// Copyright 2020, 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.*;
import java.sql.Connection;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import org.deltava.acars.online.*;
import org.deltava.beans.servinfo.NetworkInfo;

import org.deltava.dao.*;

import org.deltava.util.TaskTimer;
import org.deltava.util.cache.*;
import org.deltava.util.system.SystemData;

import org.gvagroup.ipc.WorkerState;
import org.gvagroup.jdbc.ConnectionPool;

/**
 * An ACARS worker thread to load online network status.
 * @author Luke
 * @version 10.1
 * @since 9.0
 */

public class OnlineStatusLoader extends Worker implements Thread.UncaughtExceptionHandler {

	final Cache<NetworkInfo> _cache = CacheManager.get(NetworkInfo.class, "ServInfoData");
	private final Collection<Loader> LOADERS = List.of(new VATSIMLoader(), new PilotEdgeLoader(), new IVAOLoader(), new POSCONLoader());

	private ConnectionPool _jdbcPool;

	private static final int SLEEP_INTERVAL = 30;

	/**
	 * Initializes the worker.
	 */
	public OnlineStatusLoader() {
		super("Online Status Loader", 45, OnlineStatusLoader.class);
	}

	@Override
	public void open() {
		super.open();
		_jdbcPool = (ConnectionPool) SystemData.getObject(SystemData.JDBC_POOL);
		
		// Init the update dates
		for (Loader l : LOADERS) {
			NetworkInfo inf = _cache.get(l.getNetwork());
			if ((inf != null) && (inf.getValidDate() != null))
				l.init(inf.getValidDate().minusSeconds(15), inf.getValidDate());
		}
	}

	@Override
	public void run() {
		log.info("Started");
		_status.setStatus(WorkerState.RUNNING);

		ForkJoinPool pool = new ForkJoinPool(6, new LoaderFactory(), this, false);
		while (!Thread.currentThread().isInterrupted()) {
			_status.setMessage("Downloading network data");
			_status.execute();

			try {
				long sleepTime = (SLEEP_INTERVAL * 1000);
				List<Loader> tasks = LOADERS.stream().filter(Loader::isEligible).collect(Collectors.toList());
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
					Connection c = null; tasks.removeIf(l -> !l.isUpdated());
					if (!tasks.isEmpty()) {
						_status.setMessage("Updating data validity");
						
						try {
							c = _jdbcPool.getConnection();
							SetOnlineTrack twdao = new SetOnlineTrack(c);
							for (Loader l : tasks)
								twdao.writePull(l.getNetwork(), l.getLastUpdate());
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

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		log.error(t.getName() + " Error - " + e.getMessage(), e);
	}
}