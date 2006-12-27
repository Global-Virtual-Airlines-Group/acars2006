// Copyright 2004, 2005, 2006 Global Virtual Airline Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.*;
import java.util.concurrent.*;

import org.deltava.acars.beans.*;

import org.deltava.util.system.SystemData;

/**
 * An ACARS Server task to handle writing to network connections.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class NetworkWriter extends Worker implements Thread.UncaughtExceptionHandler {

	private final List<ConnectionWriter> _writers = new ArrayList<ConnectionWriter>();
	protected final BlockingQueue<Envelope> _work = new LinkedBlockingQueue<Envelope>();
	
	protected ACARSConnectionPool _pool;
	private int _writerID = 0;
	
	private final class ConnectionWriter extends Thread {
		
		private Envelope _env;
		private volatile boolean _isBusy;
		
		ConnectionWriter(int id) {
			super("ConnectionWriter-" + String.valueOf(id));
			setDaemon(true);
		}
		
		public boolean isBusy() {
			return _isBusy;
		}
		
		public Envelope getEnvelope() {
			return _env;
		}
		
		public void run() {
			while (!isInterrupted()) {
				try {
					_env = _work.take();
					_isBusy = true;
					ACARSConnection c = _pool.get(_env.getConnectionID());
					if (c != null) {
						log.debug("Writing to " + c.getRemoteAddr());
						_status.setMessage("Writing to " + c.getUserID() + " (" + c.getRemoteAddr() + ")");
						c.queue((String) _env.getMessage());
					}
					
					// Wake up the parent thread if it's waiting on queue
					MessageStack.RAW_OUTPUT.wakeup(true);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
				}
				
				_isBusy = true;
			}
		}
	}
	
	/**
	 * Initializes the worker task.
	 */
	public NetworkWriter() {
		super("Network I/O Writer", NetworkWriter.class);
	}

	/**
	 * Opens the worker task.
	 */
	public final void open() {
		super.open();
		_pool = (ACARSConnectionPool) SystemData.getObject(SystemData.ACARS_POOL);
		
		// Create initial writer threads
		int minThreads = SystemData.getInt("acars.pool.threads.write.min", 1);
		for (int x = 0; x < minThreads; x++) {
			ConnectionWriter w = new ConnectionWriter(++_writerID);
			w.setUncaughtExceptionHandler(this);
			w.start();
			_writers.add(w);
		}
	}
	
	/**
	 * ConnectionWriter exception handler
	 * @param t the Thread with an exception
	 * @param e the exception
	 */
	public void uncaughtException(Thread t, Throwable e) {
		if ((!_writers.contains(t)) || (!(t instanceof ConnectionWriter)))
			return;
		
		// Return the envelope and log the error
		ConnectionWriter cw = (ConnectionWriter) t;
		_work.add(cw.getEnvelope());
		log.error(cw.getName() + " error - " + e.getMessage(), e);
		checkWriters();
	}

	/**
	 * Executes the thread.
	 */
	protected void $run0() throws Exception {
		log.info("Started");

		while (!Thread.currentThread().isInterrupted()) {
			_status.execute();
			
			// Loop through the raw output stack
			while (MessageStack.RAW_OUTPUT.hasNext()) {
				_status.setMessage("Dispatching - " + _writers.size() + " threads");
				Envelope env = MessageStack.RAW_OUTPUT.pop();

				// Get the connection and write the message
				if (env != null)
					_work.add(env);
			}
			
			// Check the thread pool
			checkWriters();

			// Log execution
			_status.complete();
			_status.setMessage("Idle - " + _writers.size() + " threads");

			// Wait until something is on the bean output stack
			MessageStack.RAW_OUTPUT.waitForActivity();
		}
	}
	
	/**
	 * Check the I/O writer thread pool. If there are more than 2 messages in the work queue, create a new worker up to the
	 * maximum. Once this is completed, check to see if we have more than the minimum number of idle workers. If we do, start
	 * killing idle workers. 
	 */
	protected void checkWriters() {
		log.debug("Checking I/O ConnectionWriters");
		
		// Check if we need to add more threads
		int workSize = _work.size();
		int maxThreads = SystemData.getInt("acars.pool.threads.write.max", 2);
		if ((workSize > 2) && (_writers.size() < maxThreads)) {
			ConnectionWriter w = new ConnectionWriter(++_writerID);
			w.setUncaughtExceptionHandler(this);
			w.start();
			_writers.add(w);
			log.debug("Spawned I/O " + w.getName());
		} else if (workSize > 2)
			log.warn("Work queue entries = " + workSize + ", pool size = " + _writers.size());
		
		// Check for excessive idle threads
		int isIdle = 0;
		int minThreads = SystemData.getInt("acars.pool.threads.write.min", 1);
		for (Iterator<ConnectionWriter> i = _writers.iterator(); i.hasNext(); ) {
			ConnectionWriter cw = i.next();
			if (!cw.isAlive()) {
				log.debug("Removing terminated " + cw.getName());
				i.remove();
			} else if (!cw.isBusy() && (isIdle < minThreads))
				isIdle++;
			else if (!cw.isBusy()) {
				log.debug("Interrupting idle " + cw.getName());
				cw.interrupt();
				i.remove();
			}
		}
	}
}