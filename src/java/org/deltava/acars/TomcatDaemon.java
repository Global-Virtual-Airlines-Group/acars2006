// Copyright 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars;

import java.util.Iterator;

import org.apache.log4j.Logger;

import org.deltava.acars.workers.*;

/**
 * An ACARS Server daemon to be run in a Tomcat instance.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class TomcatDaemon extends ServerDaemon implements Runnable {

	public void run() {

		// Initialize the logger
		log = Logger.getLogger("ACARSDaemon");

		// Init the connection pool
		try {
			initACARSConnectionPool();
		} catch (ACARSException ae) {
			log.error("Cannot create ConnectionPool - " + ae.getMessage());
			return;
		}

		// Init the server tasks
		initTasks();
		initThreads();

		// Log the start of the loop
		log.info("Started");

		// Start looping
		boolean keepRunning = true;
		while (keepRunning) {
			// Go to sleep for a while - if interrupted, shut down the loop
			try {
				Thread.sleep(25000);
			} catch (InterruptedException ie) {
				keepRunning = false;
			}
			
			// Check all of the threads
			for (int x = 0; keepRunning && (x < _tasks.size()); x++) {
				Worker w = (Worker) _tasks.get(x);

				// Get the worker status
				WorkerStatus status = w.getWorkerStatus();
				int wStat = status.getStatus();
				switch (wStat) {
					case WorkerStatus.STATUS_SHUTDOWN:
						log.warn("Shutdown requested by " + w.getName());
						keepRunning = false;
						break;

					case WorkerStatus.STATUS_UNKNOWN:
					case WorkerStatus.STATUS_ERROR:
						log.warn("Error on " + w.getName() + " - " + status.getMessage());

						// Give the thread a second to get killed
						Thread t = (Thread) _threads.get(w.getClass());
						try {
							if (t.isAlive())
								t.join(500);
						} catch (InterruptedException ie) {
						}

						// Restart the worker thread
						Thread wt = new Thread(_workers, w, w.getName());
						_threads.put(w.getClass(), wt);
						wt.start();
						break;
				}
			}
		}

		// Interrupt the threads
		_workers.interrupt();

		// Try to close the workers down
		for (Iterator i = _tasks.iterator(); i.hasNext();) {
			Worker w = (Worker) i.next();

			// Wait for the thread to die if it hasn't yet
			Thread t = (Thread) _threads.get(w.getClass());
			try {
				if (t.isAlive())
					t.join(500);
			} catch (InterruptedException ie) {
			}

			//Close the thread
			log.debug("Stopping " + w.getName());
			w.close();
		}

		// Display shutdown message
		log.info("Terminated");
	}
}