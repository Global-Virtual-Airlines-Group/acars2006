// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.*;
import java.util.concurrent.*;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;

import org.gvagroup.ipc.WorkerStatus;

/**
 * An ACARS Server worker is the runnable task for an ACARS server thread.
 * @author Luke
 * @version 4.0
 * @since 1.0
 */

public abstract class Worker implements Runnable {
	
	protected static final BlockingQueue<TextEnvelope> RAW_INPUT = new LinkedBlockingQueue<TextEnvelope>();
	public static final BlockingQueue<MessageEnvelope> MSG_INPUT = new LinkedBlockingQueue<MessageEnvelope>();
	public static final BlockingQueue<MessageEnvelope> MSG_OUTPUT = new PriorityBlockingQueue<MessageEnvelope>();
	public static final BlockingQueue<OutputEnvelope> RAW_OUTPUT = new PriorityBlockingQueue<OutputEnvelope>();
	public static final BlockingQueue<MessageEnvelope> MP_UPDATE = new LinkedBlockingQueue<MessageEnvelope>();
	
	protected Logger log;
	private String _name;
	
	protected ACARSConnectionPool _pool;
	protected WorkerStatus _status;
	
	/**
	 * Initializes the Worker.
	 * @param name the thread name
	 * @param sortOrder the sort order
	 * @param loggerClass the logging class
	 * @throws NullPointerException if name is null
	 */
	protected Worker(String name, int sortOrder, Class<?> loggerClass) {
		super();
		_name = name.trim();
		log = Logger.getLogger(loggerClass);
		_status = new WorkerStatus(name, sortOrder);
	}

	/**
	 * Returns the status of the Worker and any child threads. 
	 * @return a List of WorkerStatus beans, with the worker status always first
	 */
	public List<WorkerStatus> getStatus() {
		return Collections.singletonList(_status);
	}
	
	/**
	 * Returns the thread name.
	 * @return the thread name
	 */
	public final String getName() {
		return _name;
	}
	
	/**
	 * Sets the ACARS Connection Pool for this worker to use.
	 * @param pool the Connection Pool
	 */
	public final void setConnectionPool(ACARSConnectionPool pool) {
		_pool = pool;
	}

	/**
	 * Initializes the Worker and grabs the ACARS Connection Pool.
	 */
	public void open() {
		_status.setStatus(WorkerStatus.STATUS_INIT);
		_status.setMessage("Initializing");
	}

	/**
	 * Closes the Worker.
	 */
	public void close() {
		_status.setStatus(WorkerStatus.STATUS_UNKNOWN);
		_status.setMessage("Shut Down");
		log.info("Shut Down");
	}
}