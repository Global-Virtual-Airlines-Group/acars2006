// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.pool;

import java.util.*;
import java.util.concurrent.*;

import org.deltava.acars.beans.*;

/**
 * A Thread Pool executor that implements built-in queueing. This allows the thread pool to
 * continue to take work units even if the dynamic thread pool 
 * @author Luke
 * @version 2.0
 * @since 2.0
 */

public class QueueingThreadPool extends ThreadPoolExecutor {
	
	private PoolWorkerFactory _tFactory;
	
	private final Map<Integer, LatencyWorkerStatus> _status = new ConcurrentHashMap<Integer, LatencyWorkerStatus>();
	protected final BlockingQueue<PoolWorker> _queuedEntries = new LinkedBlockingQueue<PoolWorker>();

	class QueueHandler implements RejectedExecutionHandler {
		public void rejectedExecution(Runnable r, ThreadPoolExecutor pool) {
			if ((r instanceof PoolWorker) && (!pool.isTerminating()))
				_queuedEntries.add((PoolWorker) r);
		}
	}
	
	/**
	 * Initializes the Thread Pool. Subclasses should set their own Thread factory.
	 * @param coreSize the number of core threads
	 * @param maxSize the maximum number of threads
	 * @param keepAliveTime each thread's idle keepalive time in milliseconds
	 */
	public QueueingThreadPool(int coreSize, int maxSize, long keepAliveTime, String name) {
		super(coreSize, maxSize, keepAliveTime, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(4));
		_tFactory = new PoolWorkerFactory(name);
		setThreadFactory(_tFactory);
		setRejectedExecutionHandler(new QueueHandler());
	}
	
	/**
	 * Returns the status counters for each worker  bucket.
	 * @return a Collection of WorkerStatus beans
	 */
	public Collection<WorkerStatus> getWorkerStatus() {
		return new TreeSet<WorkerStatus>(_status.values());
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
			ws = new LatencyWorkerStatus(pt.getName(), 1024);
			_status.put(Integer.valueOf(pt.getID()), ws);
		}
		
		// Inject the worker status
		PoolWorker pw = (PoolWorker) r;
		pw.setStatus(ws);
	}

	/**
	 * After a thread has completed execution, if there are queued entries they are removed from
	 * the queue and executed again.
	 */
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		BlockingQueue<Runnable> workQueue = getQueue();
		while (!_queuedEntries.isEmpty() && (workQueue.size() < 4)) {
			PoolWorker pw = _queuedEntries.poll();
			if (pw != null)
				execute(pw);
		}
	}
}