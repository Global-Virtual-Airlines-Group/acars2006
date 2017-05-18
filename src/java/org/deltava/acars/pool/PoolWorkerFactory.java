// Copyright 2007, 2014, 2016, 2017 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.pool;

import java.util.*;
import java.util.concurrent.*;

import org.gvagroup.ipc.*;

/**
 * A factory to generate thread pool worker threads. This thread factory resuses thread IDs to not allow thread IDs to continue incrementing. 
 * @author Luke
 * @version 7.4
 * @since 2.0
 */

public class PoolWorkerFactory implements ThreadFactory {
	
	private final Collection<Integer> _IDs = new TreeSet<Integer>();
	protected final String _name;
	
	class PoolThread extends Thread implements Thread.UncaughtExceptionHandler {
		private final int _id;
		private PoolWorker.PoolWorkerDeathHandler _deathHandler;
		private WorkerStatus _status;
		
		PoolThread(int id, Runnable r, String name) {
			super(r, name);
			setDaemon(true);
			setUncaughtExceptionHandler(this);
			_id = id;
		}
		
		public int getID() {
			return _id;
		}
		
		public boolean isNew() {
			return (_status == null);
		}
		
		public void setStatus(WorkerStatus ws) {
			_status = ws;
		}
		
		public void setDeathHandler(PoolWorker.PoolWorkerDeathHandler handler) {
			_deathHandler = handler;
		}
		
		@Override
		public void run() {
			try {
				super.run();
				if (_status != null) {
					_status.setStatus(WorkerState.SHUTDOWN);
					_status.setAlive(false);
				}
			} catch (IllegalMonitorStateException ime) {
				// empty
			} finally {
				removeID(_id);
				if (_deathHandler != null)
					_deathHandler.workerTerminated(this, null);
			}
		}
		
		@Override
		public void uncaughtException(Thread t, Throwable e) {
			if ((this == t) && (_deathHandler != null))
				_deathHandler.workerTerminated(this, e);
		}
	}
	
	/**
	 * Initializes the thread factory.
	 * @param name the thread pool name
	 */
	PoolWorkerFactory(String name) {
		super();
		_name = name;
	}
	
	/**
	 * Returns the factory name.
	 */
	@Override
	public String toString() {
		return _name + "WorkerFactory";
	}

	/**
	 * Spawns a new thread pool thread.
	 * @param r the Runnable to wrap the Thread around
	 */
	@Override
	public Thread newThread(Runnable r) {
		int id = getNextID();
		return new PoolThread(id, r, _name + "-" + String.valueOf(id));
	}

	/**
	 * Helper method to return an available thread ID.
	 * @return the next available thread ID
	 */
	synchronized int getNextID() {
		int expectedID = 1;
		for (Integer id :_IDs) {
			if (id.intValue() > expectedID) break;
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
}