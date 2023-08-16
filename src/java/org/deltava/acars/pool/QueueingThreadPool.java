// Copyright 2007, 2008, 2009, 2010, 2011, 2016, 2017, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.pool;

import java.util.*;
import java.util.concurrent.*;
import java.time.Instant;

import org.apache.logging.log4j.*;

import org.deltava.acars.beans.*;
import org.gvagroup.ipc.WorkerState;
import org.gvagroup.ipc.WorkerStatus;

/**
 * A Thread Pool executor that implements built-in queueing. This allows the thread pool to continue to take work units even if the dynamic thread pool reaches its maximum size. 
 * @author Luke
 * @version 11.1
 * @since 2.0
 */

public class QueueingThreadPool extends ThreadPoolExecutor implements PoolWorker.PoolWorkerDeathHandler {
	
	protected final Logger log;
	private final PoolWorkerFactory _tFactory;
	private int _sortOrderBase;
	
	protected final Map<Integer, LatencyWorkerStatus> _status = new ConcurrentHashMap<Integer, LatencyWorkerStatus>();
	protected final BlockingQueue<PoolQueueEntry> _queuedEntries = new PriorityBlockingQueue<PoolQueueEntry>(32);

	/**
	 * A class to store pooled worker operations in FIFO order.
	 */
	class PoolQueueEntry implements Comparable<PoolQueueEntry> {
		
		private final PoolWorker _worker;
		private final Instant _queuedOn = Instant.now();
		
		PoolQueueEntry(PoolWorker worker) {
			super();
			_worker = worker;
		}
		
		public PoolWorker getWorker() {
			return _worker;
		}
		
		public Instant getQueuedOn() {
			return _queuedOn;
		}
		
		@Override
		public int compareTo(PoolQueueEntry pqe2) {
			return _queuedOn.compareTo(pqe2._queuedOn);
		}
	}
	
	/**
	 * This queues rejected tasks for later execution.
	 */
	class QueueHandler implements RejectedExecutionHandler {
		private long _lastEntryTime;
		private boolean _queueBackup;
		
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor pool) {
			if ((r instanceof PoolWorker pw) && (!pool.isTerminating())) {
				long now = System.currentTimeMillis();
				_queuedEntries.add(new PoolQueueEntry(pw));

				// Check if we log
				int size = _queuedEntries.size();
				boolean queueBackup = (size > 40);
				if (queueBackup) {
					PoolQueueEntry qe = _queuedEntries.peek();
					long wait = now - qe.getQueuedOn().toEpochMilli();
					queueBackup &= (wait > 1500);
				}
				
				if (!_queueBackup && queueBackup) {
					_queueBackup = true;
					log.error("Queue appears backed up, size = " + size);
					for (Map.Entry<Integer, LatencyWorkerStatus> e : _status.entrySet())
						log.error("Worker " + e.getKey() + " = " + e.getValue().getMessage());
				} else if (((now - _lastEntryTime) > 2500) && (size > 3)) 
					log.warn("Thread pool full - queueing entry #" + size);
				else
					_queueBackup &= queueBackup;
				
				_lastEntryTime = now;
			}
		}
	}
	
	/**
	 * Initializes the Thread Pool. Subclasses should set their own Thread factory.
	 * @param coreSize the number of core threads
	 * @param maxSize the maximum number of threads
	 * @param keepAliveTime each thread's idle keepalive time in milliseconds
	 * @param logClass the Logging class name
	 */
	public QueueingThreadPool(int coreSize, int maxSize, long keepAliveTime, Class<?> logClass) {
		super(coreSize, Math.max(coreSize, maxSize), keepAliveTime, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(4, true));
		log = LogManager.getLogger(logClass);
		_tFactory = new PoolWorkerFactory(logClass.getSimpleName());
		setThreadFactory(_tFactory);
		setRejectedExecutionHandler(new QueueHandler());
	}
	
	/**
	 * Sets the base sort order for pool thread {@link WorkerStatus} beans.
	 * @param sortBase the sort order base value
	 */
	public void setSortBase(int sortBase) {
		_sortOrderBase = Math.max(0, sortBase);
	}
	
	/**
	 * Returns the status counters for each worker bucket.
	 * @return a Collection of WorkerStatus beans
	 */
	public Collection<WorkerStatus> getWorkerStatus() {
		return new TreeSet<WorkerStatus>(_status.values());
	}
	
	/**
	 * Returns the number of tasks queued to be executed.
	 * @return the number of tasks
	 */
	public int getTasksWaiting() {
		return _queuedEntries.size();
	}
	
	/**
	 * Assign the ID to the thread.
	 * @param t the Thread
	 * @param r the Runnable
	 */
	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);
		if ((!(r instanceof PoolWorker pw)) || (!(t instanceof PoolWorkerFactory.PoolThread pt)))
			return;
		
		// Get the worker status
		LatencyWorkerStatus ws = _status.get(Integer.valueOf(pt.getID()));
		if (ws == null) {
			ws = new LatencyWorkerStatus(pt.getName(), _sortOrderBase + pt.getID(), 1024);
			_status.put(Integer.valueOf(pt.getID()), ws);
		}
		
		// Log thread startup
		if (pt.isNew()) {
			log.info("Spawning thread " + pt.getName());
			pt.setDeathHandler(this);
			ws.clear();
		}
		
		// Inject the worker status
		ws.setStatus(WorkerState.RUNNING);
		ws.execute();
		pw.setStatus(ws);
		pt.setStatus(ws);
	}

	/**
	 * After a thread has completed execution, if there are queued entries they are removed from the queue and executed again.
	 */
	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		if (r instanceof PoolWorker pw) {
			WorkerStatus ws = pw.getStatus();
			ws.complete();
			ws.setMessage("Idle");
		}
		
		// See if there are additional tasks queued up
		BlockingQueue<Runnable> workQueue = getQueue();
		while (!_queuedEntries.isEmpty() && (workQueue.remainingCapacity() > 0)) {
			PoolQueueEntry pw = _queuedEntries.poll();
			if (pw != null)
				execute(pw.getWorker());
		}
	}
	
	@Override
	public void workerTerminated(PoolWorkerFactory.PoolThread pt, Throwable e) {
		_tFactory.removeID(pt.getID());
		if (e != null)
			log.error(pt.getName() + " - "  + e.getMessage(), e);
		else
			log.info("{} shut down", pt.getName());
	}
}