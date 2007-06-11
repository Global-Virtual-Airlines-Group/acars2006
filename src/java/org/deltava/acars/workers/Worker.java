// Copyright 2004, 2005, 2006, 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.*;
import java.util.concurrent.*;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.util.system.SystemData;

/**
 * An ACARS Server worker is the runnable task for an ACARS server thread.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public abstract class Worker implements Runnable {
	
	protected static final BlockingQueue<TextEnvelope> RAW_INPUT = new LinkedBlockingQueue<TextEnvelope>();
	public static final BlockingQueue<MessageEnvelope> MSG_INPUT = new LinkedBlockingQueue<MessageEnvelope>();
	public static final BlockingQueue<MessageEnvelope> MSG_OUTPUT = new PriorityBlockingQueue<MessageEnvelope>();
	protected static final BlockingQueue<TextEnvelope> RAW_OUTPUT = new PriorityBlockingQueue<TextEnvelope>();
	
	protected Logger log;
	private String _name;
	
	protected ACARSConnectionPool _pool;
	protected WorkerStatus _status;
	
	/**
	 * Initializes the Worker.
	 * @param name the thread name
	 * @param loggerClassName the logging class name
	 * @throws NullPointerException if name is null
	 */
	protected Worker(String name, String loggerClassName) {
		_name = name.trim();
		_status = new WorkerStatus(name);
		log = Logger.getLogger(loggerClassName);
	}
	
	/**
	 * Initializes the Worker.
	 * @param name the thread name
	 * @param loggerClass the logging class
	 * @throws NullPointerException if name is null
	 */
	protected Worker(String name, Class loggerClass) {
		this(name, loggerClass.getName());
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
	 * Initializes the Worker and grabs the ACARS Connection Pool.
	 */
	public void open() {
		_status.setStatus(WorkerStatus.STATUS_INIT);
		_status.setMessage("Initializing");
		_pool = (ACARSConnectionPool) SystemData.getObject(SystemData.ACARS_POOL);
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