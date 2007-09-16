// Copyright 2004, 2005, 2006, 2007 Global Virtual Airline Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.*;
import java.util.concurrent.*;

import org.deltava.acars.beans.*;

import org.deltava.util.ThreadUtils;
import org.deltava.util.system.SystemData;

/**
 * An ACARS Server task to handle writing to network connections.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class NetworkWriter extends Worker implements Thread.UncaughtExceptionHandler {

	private final List<ConnectionWriter> _writers = new ArrayList<ConnectionWriter>();
	protected final Map<Integer, WorkerStatus> _writerStatus = new TreeMap<Integer, WorkerStatus>();
	protected final BlockingQueue<TextEnvelope> _work = new PriorityBlockingQueue<TextEnvelope>();

	private final class ConnectionWriter extends Thread {

		private TextEnvelope _env;
		private WorkerStatus _cwStatus;
		private boolean _isBusy;
		private boolean _isDynamic;

		private final LatencyTracker _latency = new LatencyTracker(1024);

		ConnectionWriter(int id, boolean isDynamic) {
			super("ConnectionWriter-" + String.valueOf(id));
			setDaemon(true);
			_cwStatus = _writerStatus.get(Integer.valueOf(id));
			_isDynamic = isDynamic;
			if (_cwStatus == null) {
				_cwStatus = new WorkerStatus(getName());
				_writerStatus.put(Integer.valueOf(id), _cwStatus);
			}

			_cwStatus.setStatus(WorkerStatus.STATUS_INIT);
		}

		public synchronized boolean isBusy() {
			return _isBusy;
		}

		private synchronized void setBusy(boolean isBusy) {
			_isBusy = isBusy;
		}

		public boolean isDynamic() {
			return _isDynamic;
		}

		public TextEnvelope getEnvelope() {
			return _env;
		}

		public WorkerStatus getStatus() {
			return _cwStatus;
		}

		public void run() {
			_cwStatus.setAlive(true);
			_cwStatus.setStatus(WorkerStatus.STATUS_START);
			while (!isInterrupted()) {
				_cwStatus.setMessage("Idle - average latency " + _latency.getLatency() + "ms");

				try {
					_env = _work.take();
					_cwStatus.execute();
					setBusy(true);
					ACARSConnection c = _pool.get(_env.getConnectionID());
					if (c != null) {
						log.debug("Writing to " + c.getRemoteAddr());
						_cwStatus.setMessage("Writing to " + c.getUserID() + " - " + c.getRemoteHost());
						c.queue(_env.getMessage());

						// Check latency
						long envLatency = System.currentTimeMillis() - _env.getTime();
						_latency.add(envLatency);
						if (envLatency > 2500) {
							log.warn(getName() + " high latency delivering to " + c.getUserID() + " ("
									+ envLatency + "ms)");
							checkWriters(false);
						}
					}
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
				} finally {
					_cwStatus.complete();
					setBusy(false);
				}

				// Check if we have too many idle threads
				if (_work.isEmpty()) {
					_cwStatus.setMessage("Checking Thread Pool");
					checkWriters(true);
				}
			}

			_cwStatus.setAlive(false);
			_cwStatus.setStatus(WorkerStatus.STATUS_SHUTDOWN);
			_cwStatus.setMessage("Shut Down");
		}
	}

	/**
	 * Initializes the worker task.
	 */
	public NetworkWriter() {
		super("Network I/O Writer", NetworkWriter.class);
	}

	/**
	 * Opens the worker task and initializes the ConnectionWriter thread pool.
	 * @see Worker#open()
	 */
	public final synchronized void open() {
		super.open();

		// Create initial writer threads
		int minThreads = SystemData.getInt("acars.pool.threads.write.min", 1);
		for (int x = 0; x < minThreads; x++)
			spawnWorker(false);

		log.info("Started " + _writers.size() + " ConnectionWriter threads");
	}

	/**
	 * Closes the worker task. All ConnectionWriter thrads will be shut down.
	 * @see Worker#close()
	 */
	public final synchronized void close() {
		_status.setStatus(WorkerStatus.STATUS_SHUTDOWN);
		for (Iterator<? extends Thread> i = _writers.iterator(); i.hasNext();) {
			Thread cw = i.next();
			ThreadUtils.kill(cw, 750);
		}

		super.close();
	}

	/**
	 * Returns the status of this Worker and the Connection writers.
	 * @return a List of WorkerStatus beans, with this Worker's status first
	 */
	public final List<WorkerStatus> getStatus() {
		List<WorkerStatus> results = new ArrayList<WorkerStatus>(super.getStatus());
		results.addAll(_writerStatus.values());
		return results;
	}

	/**
	 * ConnectionWriter thread exception handler.
	 * @param t the Thread with an exception
	 * @param e the exception
	 */
	public void uncaughtException(Thread t, Throwable e) {
		if ((!_writers.contains(t)) || (!(t instanceof ConnectionWriter)))
			return;

		// Return the envelope and log the error
		ConnectionWriter cw = (ConnectionWriter) t;
		_work.add(cw.getEnvelope());
		log.error(cw.getName() + " error - " + e.getMessage(), e);
		checkWriters(false);
	}

	/**
	 * Executes the Thread.
	 */
	public void run() {
		log.info("Started");
		_status.setStatus(WorkerStatus.STATUS_START);
		while (!Thread.currentThread().isInterrupted()) {
			try {
				_status.setMessage("Idle - " + _writers.size() + " threads");
				TextEnvelope env = RAW_OUTPUT.take();
				_status.execute();
				_status.setMessage("Dispatching - " + _writers.size() + " threads");
				while (env != null) {
					_work.add(env);
					env = RAW_OUTPUT.poll();
				}

				// Check the thread pool - wait 100ms for the writers to wake up
				ThreadUtils.sleep(100);
				checkWriters(false);

				// Log execution
				_status.complete();
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * Helper method to get the next available worker ID.
	 */
	private int getNextAvailableID() {
		int id = 0;
		for (Iterator<Integer> i = _writerStatus.keySet().iterator(); i.hasNext();) {
			Integer workerID = i.next();
			id++;
			if (workerID.intValue() > id)
				return id;

			// Check if we're shut down
			WorkerStatus status = _writerStatus.get(workerID);
			if (status.getStatus() == WorkerStatus.STATUS_SHUTDOWN)
				return id;
		}

		return ++id;
	}

	/**
	 * Helper method to spawn a new connection worker thread.
	 */
	private void spawnWorker(boolean isDynamic) {
		ConnectionWriter w = new ConnectionWriter(getNextAvailableID(), isDynamic);
		w.setUncaughtExceptionHandler(this);
		w.start();
		_writers.add(w);
		if (log.isDebugEnabled())
			log.debug("Spawned I/O " + w.getName());
	}

	/**
	 * Check the I/O writer thread pool. If there are more than 2 messages in the work queue or the oldest
	 * message in the work queue has been waiting for over 2500ms, create a new worker up to the maximum. Once
	 * this is completed, check to see if we have more than the minimum number of idle workers. If we do,
	 * start killing idle workers.
	 * @param killOnly TRUE if we should only prune idle connections, otherwise FALSE
	 */
	protected synchronized void checkWriters(boolean killOnly) {
		log.debug("Checking I/O ConnectionWriters");
		int minThreads = SystemData.getInt("acars.pool.threads.write.min", 1);
		int workSize = _work.size();

		// Check if we need to add more threads
		if (!killOnly) {
			Envelope env = _work.peek();
			long envAge = (env == null) ? -1 : (System.currentTimeMillis() - env.getTime());
			int maxThreads = SystemData.getInt("acars.pool.threads.write.max", 2);
			if ((workSize > 2) && (_writers.size() < maxThreads) && (envAge > 1250)) {
				spawnWorker(true);
				log.info("ConnectionWriter Pool size increased to " + _writers.size());
			} else if ((workSize > 2) && (envAge > 1500))
				log.warn("Work queue entries = " + workSize + ", pool size = " + _writers.size());
			else if (workSize == 1) {
				if ((envAge > 2500) && (_writers.size() < maxThreads)) {
					spawnWorker(true);
					log.info("ConnectionWriter Pool size increased to " + _writers.size());
				} else if (envAge > 2500)
					log.warn("Work queue head age = " + envAge + "ms, pool size = " + _writers.size());
			} else if (_writers.size() < minThreads) {
				log.warn("ConnectionWriter Pool size=" + _writers.size() + ", minimum=" + minThreads);
				while (_writers.size() < minThreads)
					spawnWorker(false);
			}
		}

		// Check for excessive idle threads
		if (workSize < (minThreads + 1)) {
			for (Iterator<ConnectionWriter> i = _writers.iterator(); i.hasNext();) {
				ConnectionWriter cw = i.next();
				if (!cw.isAlive()) {
					i.remove();
					if (log.isDebugEnabled())
						log.debug("Removing terminated " + cw.getName());
				} else if (!cw.isBusy() && cw.isDynamic()) {
					log.debug("Interrupting idle " + cw.getName());
					cw.interrupt();
					i.remove();
					log.info("ConnectionWriter Pool size decreased to " + _writers.size());
				}
			}
		}
	}
}