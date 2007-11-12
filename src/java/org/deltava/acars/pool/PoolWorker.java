// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.pool;

import org.deltava.acars.beans.LatencyWorkerStatus;

/**
 * An interface to describe Thread Pool workers.
 * @author Luke
 * @version 2.0
 * @since 2.0
 */

public interface PoolWorker extends Runnable {

	/**
	 * Returns this worker's status bean.
	 * @return a WorkerStatus bean
	 */
	public LatencyWorkerStatus getStatus();
	
	/**
	 * Injects a status bean for the worker to use.
	 * @param ws the WorkerStatus bean to use
	 */
	public void setStatus(LatencyWorkerStatus ws);
	
	/**
	 * Returns the worker name.
	 * @return the name
	 */
	public String getName();
}