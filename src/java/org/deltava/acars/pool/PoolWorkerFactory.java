// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.pool;

import java.util.*;
import java.util.concurrent.*;

import org.apache.log4j.Logger;

/**
 * A factory to generate thread pool worker threads. This thread factory resuses thread IDs
 * to allow workers 
 * @author Luke
 * @version 2.0
 * @since 2.0
 */

public class PoolWorkerFactory implements ThreadFactory, Thread.UncaughtExceptionHandler {
	
	private Logger log;
	
	protected ThreadGroup _tg;
	private final Collection<Integer> _IDs = new TreeSet<Integer>();
	
	private String _name;
	
	class PoolThread extends Thread {
		private int _id;
		
		/**
		 * Initializes the thread.
		 * @param id the worker ID
		 * @param r the task to run
		 * @param name the thread name
		 */
		PoolThread(int id, Runnable r, String name) {
			super(r, name);
			_id = id;
		}
		
		/**
		 * Returns the worker ID.
		 * @return the ID
		 */
		public int getID() {
			return _id;
		}
		
		public void run() {
			super.run();
			removeID(_id);
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
		int id = getNextID();
		PoolThread t = new PoolThread(id, r, _name + "-" + String.valueOf(id));
		t.setUncaughtExceptionHandler(this);
		return t;
	}

	/**
	 * Helper method to return an available thread ID.
	 * @return the next available thread ID
	 */
	synchronized int getNextID() {
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
	synchronized void removeID(int id) {
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