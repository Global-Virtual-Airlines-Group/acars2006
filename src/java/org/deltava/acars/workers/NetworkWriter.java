// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 Global Virtual Airline Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.*;
import java.util.concurrent.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.pool.*;

import org.deltava.util.system.SystemData;

import org.gvagroup.ipc.WorkerStatus;

/**
 * An ACARS Server task to handle writing to network connections.
 * @author Luke
 * @version 4.0
 * @since 1.0
 */

public class NetworkWriter extends Worker {

	private QueueingThreadPool _ioPool;

	private final class ConnectionWriter extends PoolWorker {

		private ACARSConnection _con;
		private OutputEnvelope _env;

		ConnectionWriter(ACARSConnection ac, OutputEnvelope env) {
			super();
			_con = ac;
			_env = env;
		}
		
		public String getName() {
			return "ConnectionWriter";
		}
		
		public void run() {
			_status.setMessage("Writing to " + _con.getUserID());
			if (_env instanceof TextEnvelope)
				_con.write((String) _env.getMessage());
			else if (_env instanceof BinaryEnvelope)
				_con.write((byte[]) _env.getMessage());
			else
				log.warn("Unknown envelope type - " + _env.getClass().getSimpleName());
			
			_status.add(System.nanoTime() - _env.getTime());
		}
	}

	/**
	 * Initializes the worker task.
	 */
	public NetworkWriter() {
		super("Network I/O Writer", 70, NetworkWriter.class);
	}

	/**
	 * Opens the worker task and initializes the ConnectionWriter thread pool.
	 * @see Worker#open()
	 */
	@Override
	public final void open() {
		super.open();

		// Create initial writer threads
		int minThreads = Math.max(1, SystemData.getInt("acars.pool.threads.write.min", 1));
		int maxThreads = Math.max(minThreads, SystemData.getInt("acars.pool.threads.write.max", minThreads));
		_ioPool = new QueueingThreadPool(minThreads, maxThreads, 1500, NetworkWriter.class);
		_ioPool.allowCoreThreadTimeOut(false);
		_ioPool.prestartCoreThread();
		_ioPool.setSortBase(70);
	}

	/**
	 * Closes the worker task. All ConnectionWriter thrads will be shut down.
	 * @see Worker#close()
	 */
	@Override
	public final void close() {
		_status.setStatus(WorkerStatus.STATUS_SHUTDOWN);
		
		// Wait for the pool to shut down
		try {
			_ioPool.shutdown();
			_ioPool.awaitTermination(1750, TimeUnit.MILLISECONDS);
		} catch (InterruptedException ie) {
			log.warn("Interrupted Pool Termination");
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
	@Override
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
				OutputEnvelope env = RAW_OUTPUT.poll(30, TimeUnit.SECONDS);
				_status.execute();
				_status.setMessage("Dispatching - " + _ioPool.getPoolSize() + " threads");
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