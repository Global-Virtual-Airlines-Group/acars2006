// Copyright 2007, 2008, 2009, 2010 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.pool;

import java.util.*;
import java.util.concurrent.*;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;

import org.gvagroup.ipc.WorkerStatus;

/**
 * A Thread Pool executor that implements built-in queueing. This allows the thread pool to
 * continue to take work units even if the dynamic thread pool reaches its maximum size. 
 * @author Luke
 * @version 3.1
 * @since 2.0
 */

public class QueueingThreadPool extends ThreadPoolExecutor implements PoolWorker.PoolWorkerDeathHandler {
	
	protected Logger log;
	private PoolWorkerFactory _tFactory;
	private int _sortOrderBase;
	
	protected final Map<Integer, LatencyWorkerStatus> _status = new ConcurrentHashMap<Integer, LatencyWorkerStatus>();
	protected final BlockingQueue<PoolWorker> _queuedEntries = new LinkedBlockingQueue<PoolWorker>();

	/**
	 * This queues rejected tasks for later execution.
	 */
	class QueueHandler implements RejectedExecutionHandler {
		private long _lastEntryTime;
		private boolean _queueBackup;
		
		public void rejectedExecution(Runnable r, ThreadPoolExecutor pool) {
			if ((r instanceof PoolWorker) && (!pool.isTerminating())) {
				long now = System.currentTimeMillis();
				_queuedEntries.add((PoolWorker) r);

				// Check if we log
				int size = _queuedEntries.size();
				boolean queueBackup = (size > 40);
				if (!_queueBackup && queueBackup) {
					_queueBackup = true;
					log.error("Queue appears backed up, size = " + size);
					for (Iterator<Map.Entry<Integer, LatencyWorkerStatus>> i = _status.entrySet().iterator(); i.hasNext(); ) {
						Map.Entry<Integer, ? extends WorkerStatus> e = i.next();
						log.error("Worker " + e.getKey() + " = " + e.getValue().getMessage());
					}
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
		log = Logger.getLogger(logClass);
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
	 * Assign the ID to the thread
	 */
	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);
		if ((!(r instanceof PoolWorker)) || (!(t instanceof PoolWorkerFactory.PoolThread)))
			return;
		
		// Get the worker status
		PoolWorkerFactory.PoolThread pt = (PoolWorkerFactory.PoolThread) t;
		LatencyWorkerStatus ws = _status.get(Integer.valueOf(pt.getID()));
		if (ws == null) {
			ws = new LatencyWorkerStatus(pt.getName(), _sortOrderBase + pt.getID(), 1024);
			_status.put(Integer.valueOf(pt.getID()), ws);
		}
		
		// Log thread startup
		if (pt.isNew()) {
			log.info("Spawning thread " + pt.getName());
			pt.setDeathHandler(this);
		}
		
		// Inject the worker status
		ws.setStatus(WorkerStatus.STATUS_START);
		ws.execute();
		PoolWorker pw = (PoolWorker) r;
		pw.setStatus(ws);
		pt.setStatus(ws);
	}

	/**
	 * After a thread has completed execution, if there are queued entries they are removed from
	 * the queue and executed again.
	 */
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		if (r instanceof PoolWorker) {
			WorkerStatus ws = ((PoolWorker) r).getStatus();
			ws.complete();
			ws.setMessage("Idle");
		}
		
		// See if there are additional tasks queued up
		BlockingQueue<Runnable> workQueue = getQueue();
		while (!_queuedEntries.isEmpty() && (workQueue.size() < 4)) {
			PoolWorker pw = _queuedEntries.poll();
			if (pw != null)
				execute(pw);
		}
	}
	
	/**
	 * Worker thread termination handler.
	 * @param pt the worker thread
	 * @param e the Exception 
	 */
	public void workerTerminated(PoolWorkerFactory.PoolThread pt, Throwable e) {
		_tFactory.removeID(pt.getID());
		if (e != null)
			log.error(pt.getName() + " - "  + e.getMessage(), e);
		else
			log.info(pt.getName() + " shut down");
	}
}