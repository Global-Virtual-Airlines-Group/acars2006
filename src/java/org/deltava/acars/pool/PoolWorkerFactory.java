// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.pool;

import java.util.*;
import java.util.concurrent.*;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;

/**
 * A factory to generate thread pool worker threads. This thread factory resuses thread IDs
 * to allow workers 
 * @author Luke
 * @version 2.0
 * @since 2.0
 */

public class PoolWorkerFactory implements ThreadFactory, Thread.UncaughtExceptionHandler {
	
	private Logger log;
	private final Map<Integer, LatencyWorkerStatus> _status = new ConcurrentHashMap<Integer, LatencyWorkerStatus>();
	
	protected ThreadGroup _tg;
	private final Collection<Integer> _IDs = new TreeSet<Integer>();
	
	private String _name;
	
	private class PoolThread extends Thread {
		private int _id;
		
		PoolThread(int id, PoolWorker work) {
			super(_tg, work, work.getStatus().toString());
			_id = id;
		}
		
		public void run() {
			super.run();
			removeID(_id);
		}
		
		public int getID() {
			return _id;
		}
	}
	
	/**
	 * Initializes the thread factory.
	 * @param name the thread pool name
	 */
	PoolWorkerFactory(String name) {
		super();
		_name = name;
		log = Logger.getLogger(PoolWorkerFactory.class.getPackage().getName() + "." + toString());
		_tg = new ThreadGroup(name + " Workers");
		_tg.setDaemon(true);
	}
	
	/**
	 * Returns the status counters for each worker  bucket.
	 * @return a Collection of WorkerStatus beans
	 */
	public Collection<WorkerStatus> getWorkerStatus() {
		return new TreeSet<WorkerStatus>(_status.values());
	}
	
	/**
	 * Returns the factory name.
	 */
	public String toString() {
		return _name + "WorkerFactory";
	}

	/**
	 * Spawns a new thread pool thread.
	 * @param r the Runnable to wrap the Thread around
	 */
	public Thread newThread(Runnable r) {
		if (!(r instanceof PoolWorker))
			return null;
		
		// Get the thread ID
		int id = getNextID();
		
		// Get the worker status
		PoolWorker pw = (PoolWorker) r;
		LatencyWorkerStatus ws = _status.get(Integer.valueOf(id));
		if (ws == null) {
			ws = new LatencyWorkerStatus(pw.getName() + "-" + String.valueOf(id), 1024);
			_status.put(Integer.valueOf(id), ws);
		}
		
		// Inject the worker status and spawn the thread
		pw.setStatus(ws);
		Thread t = new PoolThread(id, pw);
		t.setUncaughtExceptionHandler(this);
		return t;
	}

	/**
	 * Helper method to return an available thread ID.
	 * @return the next available thread ID
	 */
	protected synchronized int getNextID() {
		int expectedID = 1;
		for (Iterator<Integer> i = _IDs.iterator(); i.hasNext(); ) {
			Integer id = i.next();
			if (id.intValue() > expectedID) {
				_IDs.add(Integer.valueOf(expectedID));
				return expectedID;
			}
				
			expectedID++;
		}
		
		_IDs.add(Integer.valueOf(expectedID));
		return expectedID;
	}

	/**
	 * Helper method to free up a thread ID and make it available for allocation.
	 * @param id the thread ID
	 */
	protected synchronized void removeID(int id) {
		_IDs.remove(Integer.valueOf(id));
	}
	
	/**
	 * Uncaught exception handler.
	 * @param t the Thread
	 * @param e the exception
	 */
	public void uncaughtException(Thread t, Throwable e) {
		if (!(t instanceof PoolThread)) {
			log.error("Unknown thread type - " + t.getClass().getName());
			return;
		}
		
		// Release the ID and log
		PoolThread pt = (PoolThread) t;
		removeID(pt.getID());
		log.error(t.getName() + " - "  + e.getMessage(), e);
	}
}