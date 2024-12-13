// Copyright 2020, 2021, 2022, 2023, 2024 Global Virtual Airlines Group. All Rights Reserved.
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
import org.gvagroup.pool.ConnectionPool;

import com.newrelic.api.agent.NewRelic;

/**
 * An ACARS worker thread to load online network status.
 * @author Luke
 * @version 11.4
 * @since 9.0
 */

public class OnlineStatusLoader extends Worker implements Thread.UncaughtExceptionHandler {

	final Cache<NetworkInfo> _cache = CacheManager.get(NetworkInfo.class, "ServInfoData");
	private final Collection<Loader> LOADERS = List.of(new VATSIMLoader(), new PilotEdgeLoader(), new IVAOLoader(), new POSCONLoader());

	private ConnectionPool<Connection> _jdbcPool;

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
		_jdbcPool = SystemData.getJDBCPool();
		
		// Init the update dates
		for (Loader l : LOADERS) {
			NetworkInfo inf = _cache.get(l.getNetwork());
			if ((inf != null) && (inf.getValidDate() != null))
				l.init(inf.getValidDate().minusSeconds(15), inf.getValidDate());
		}
	}
	
	private static Runnable runnableWrapper(CountDownLatch lt, Runnable r)  {
		return new Runnable() {
			@Override
			public void run() {
				r.run();
				lt.countDown();
			}
		};
	}

	@Override
	public void run() {
		log.info("Started");
		_status.setStatus(WorkerState.RUNNING);

		try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
			while (!Thread.currentThread().isInterrupted()) {
				_status.setMessage("Downloading network data");
				_status.execute();

				try {
					long sleepTime = (SLEEP_INTERVAL * 1000);
					List<Loader> tasks = LOADERS.stream().filter(Loader::isEligible).collect(Collectors.toList());
					if (!tasks.isEmpty()) {
						CountDownLatch lt = new CountDownLatch(tasks.size());
						tasks.forEach(t -> pool.submit(runnableWrapper(lt, t)));
						TaskTimer tt = new TaskTimer();
						boolean isComplete = lt.await((SLEEP_INTERVAL - 2), TimeUnit.SECONDS);
						long ms = tt.stop();
						sleepTime -= ms;
						_status.complete();
						if (!isComplete) {
							log.warn("Pool still active after {}ms", Long.valueOf(ms));
							sleepTime += (SLEEP_INTERVAL * 1000);
						}

						// Record the update times for outage tracking
						tasks.removeIf(l -> !l.isUpdated());
						if (!tasks.isEmpty()) {
							_status.setMessage("Updating data validity");
							try (Connection c = _jdbcPool.getConnection()) {
								SetOnlineTrack twdao = new SetOnlineTrack(c);
								for (Loader l : tasks)
									twdao.writePull(l.getNetwork(), l.getLastUpdate());
							} catch (DAOException de) {
								log.atError().withThrowable(de).log("Error writing update times - {}", de.getMessage());
							}
						}
					}

					_status.setMessage("Idle");
					Thread.sleep(sleepTime);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
				} catch (Exception e) {
					log.atError().withThrowable(e).log(e.getMessage());
				}
			}
		}
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		log.atError().withThrowable(e).log("{} Error - {}", t.getName(), e.getMessage());
		NewRelic.noticeError(e, false);
	}
}