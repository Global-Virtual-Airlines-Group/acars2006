// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.pool;

import org.deltava.acars.beans.LatencyWorkerStatus;

/**
 * An abstract class to describe Thread Pool workers.
 * @author Luke
 * @version 2.0
 * @since 2.0
 */

public abstract class PoolWorker implements Runnable {

	/**
	 * The thread's status bean.
	 */
	protected LatencyWorkerStatus _status;
	
	/**
	 * Returns this worker's status bean.
	 * @return a WorkerStatus bean
	 */
	public LatencyWorkerStatus getStatus() {
		return _status;
	}
	
	/**
	 * Injects a status bean for the worker to use.
	 * @param ws the WorkerStatus bean to use
	 */
	public void setStatus(LatencyWorkerStatus ws) {
		_status = ws;
	}
	
	/**
	 * Returns the worker name.
	 * @return the name
	 */
	public abstract String getName();
	
	/**
	 * Interface for handlers of pool worker thread death. 
	 */
	public interface PoolWorkerDeathHandler {

		/**
		 * Handle the termination of a worker thread.
		 * @param pt the worker thread
		 * @param e the exception causing the thread to die
		 */
		public void workerTerminated(PoolWorkerFactory.PoolThread pt, Throwable e);
	}
}