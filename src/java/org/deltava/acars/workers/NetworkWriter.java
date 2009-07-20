// Copyright 2004, 2005, 2006, 2007, 2008, 2009 Global Virtual Airline Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.*;
import java.util.concurrent.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.pool.*;

import org.deltava.util.system.SystemData;

/**
 * An ACARS Server task to handle writing to network connections.
 * @author Luke
 * @version 2.6
 * @since 1.0
 */

public class NetworkWriter extends Worker {

	private QueueingThreadPool _ioPool;

	private final class ConnectionWriter extends PoolWorker {

		private ACARSConnection _con;
		private TextEnvelope _env;

		ConnectionWriter(ACARSConnection ac, TextEnvelope env) {
			super();
			_con = ac;
			_env = env;
		}
		
		public String getName() {
			return "ConnectionWriter";
		}
		
		public void run() {
			_status.setMessage("Writing to " + _con.getUserID());
			_con.queue(_env.getMessage());
			_status.add(System.currentTimeMillis() - _env.getTime());
		}
	}

	/**
	 * Initializes the worker task.
	 */
	public NetworkWriter() {
		super("Network I/O Writer", 60, NetworkWriter.class);
	}

	/**
	 * Opens the worker task and initializes the ConnectionWriter thread pool.
	 * @see Worker#open()
	 */
	public final void open() {
		super.open();

		// Create initial writer threads
		int minThreads = Math.max(1, SystemData.getInt("acars.pool.threads.write.min", 1));
		int maxThreads = Math.max(minThreads, SystemData.getInt("acars.pool.threads.write.max", minThreads));
		_ioPool = new QueueingThreadPool(minThreads, maxThreads, 1500, NetworkWriter.class);
		_ioPool.allowCoreThreadTimeOut(false);
		_ioPool.prestartCoreThread();
		_ioPool.setSortBase(60);
	}

	/**
	 * Closes the worker task. All ConnectionWriter thrads will be shut down.
	 * @see Worker#close()
	 */
	public final void close() {
		_status.setStatus(WorkerStatus.STATUS_SHUTDOWN);
		
		// Wait for the pool to shut down
		try {
			_ioPool.shutdown();
			_ioPool.awaitTermination(1750, TimeUnit.MILLISECONDS);
		} catch (InterruptedException ie) {
			log.warn(ie.getMessage());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			super.close();
		}
	}

	/**
	 * Returns the status of this Worker and the Connection writers.
	 * @return a List of WorkerStatus beans, with this Worker's status first
	 */
	public final List<WorkerStatus> getStatus() {
		List<WorkerStatus> results = new ArrayList<WorkerStatus>(super.getStatus());
		results.addAll(_ioPool.getWorkerStatus());
		return results;
	}

	/**
	 * Executes the Thread.
	 */
	public void run() {
		log.info("Started");
		_status.setStatus(WorkerStatus.STATUS_START);
		while (!Thread.currentThread().isInterrupted()) {
			try {
				_status.setMessage("Idle - " + _ioPool.getPoolSize() + " threads");
				TextEnvelope env = RAW_OUTPUT.take();
				_status.execute();
				_status.setMessage("Dispatching - " + _ioPool.getActiveCount() + " threads");
				while (env != null) {
					ACARSConnection ac = _pool.get(env.getConnectionID());
					if (ac != null)
						_ioPool.execute(new ConnectionWriter(ac, env));
	
					env = RAW_OUTPUT.poll();
				}

				// Log execution
				_status.complete();
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}
}