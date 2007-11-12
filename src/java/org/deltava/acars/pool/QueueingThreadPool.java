// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.pool;

import java.util.concurrent.*;

/**
 * A Thread Pool executor that implements built-in queueing. This allows the thread pool to
 * continue to take work units even if the dynamic thread pool 
 * @author Luke
 * @version 2.0
 * @since 2.0
 */

public class QueueingThreadPool extends ThreadPoolExecutor {
	
	private PoolWorkerFactory _tFactory;
	protected final BlockingQueue<PoolWorker> _queuedEntries = new LinkedBlockingQueue<PoolWorker>();

	class QueueHandler implements RejectedExecutionHandler {
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			if (r instanceof PoolWorker)
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
	 * After a thread has completed execution, if there are queued entries they are removed from
	 * the queue and executed again.
	 */
	protected void afterExecute(Runnable r, Throwable t) {
		while (!_queuedEntries.isEmpty() && (getQueue().size() < 4)) {
			PoolWorker pw = _queuedEntries.poll();
			if (pw != null)
				execute(pw);
		}
	}
	
	/**
	 * Queues an element for execution.
	 * @param work the work entry
	 */
	public void queue(PoolWorker work) {
		execute(work);
	}
}