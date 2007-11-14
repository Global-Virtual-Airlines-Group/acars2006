// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.pool;

/**
 * An interface to describe Thread Pool worker thread termination handlers.
 * @author Luke
 * @version 2.0
 * @since 2.0
 */

public interface PoolWorkerDeathHandler {

	/**
	 * Handle the termination of a worker thread.
	 * @param pt the worker thread
	 * @param e the exception causing the thread to die
	 */
	public void workerTerminated(PoolWorkerFactory.PoolThread pt, Throwable e);
}