// Copyright 2004,2005 Luke J. Kolin
package org.deltava.acars;

import java.util.*;

import org.apache.log4j.LogManager;

import org.deltava.acars.workers.*;

import org.deltava.util.system.SystemData;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class StandaloneDaemon extends ServerDaemon {

	public static void main(String[] args) {

		// Startup message
		System.out.println("Delta Virtual Airlnes ACARS Server v" + String.valueOf(ACARSInfo.MAJOR_VERSION) + "."
				+ String.valueOf(ACARSInfo.MINOR_VERSION));
		System.out.println("(C) 2003, 2004, 2005 " + ACARSInfo.AUTHOR_NAME + ". All Rights Reserved.\n");

		// Initialize the logger
		initLog(StandaloneDaemon.class);

		// Start everything up
		SystemData.init();
		initAuthenticator();
		initConnectionPool();
		initAirports();

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
			// Check all of the threads
			for (Iterator wi = _tasks.iterator(); wi.hasNext();) {
				Worker w = (Worker) wi.next();

				// Get the worker status
				WorkerStatus status = w.getWorkerStatus();
				int wStat = status.getStatus();
				switch (wStat) {
					case WorkerStatus.STATUS_SHUTDOWN:
						log.warn("Shutdown requested by " + w.getName());
						keepRunning = false;
						break;

					case WorkerStatus.STATUS_ERROR:
						log.warn("Error on " + w.getName() + " - " + status.getMessage());

						// Give the thread a second to get killed
						Thread t = _threads.get(w.getClass());
						try {
							if (t.isAlive())
								t.join(500);
						} catch (InterruptedException ie) {
						}

						// Create a new thread based on this worker
						Thread wt = new Thread(_workers, w, w.getName());
						_threads.put(w.getClass(), wt);
						wt.start();
						break;
				}
			}

			// Go to sleep for a while - if interrupted, shut down the loop
			try {
				Thread.sleep(2500);
			} catch (InterruptedException ie) {
				keepRunning = false;
			}
		}

		// Interrupt the threads
		_workers.interrupt();

		// Try to close the workers down
		for (Iterator i = _tasks.iterator(); i.hasNext();) {
			Worker w = (Worker) i.next();

			// Wait for the thread to die if it hasn't yet
			Thread t = _threads.get(w.getClass());
			try {
				if (t.isAlive())
					t.join(500);
			} catch (InterruptedException ie) {
			}

			//Close the thread
			log.info("Stopping " + w.getName());
			w.close();
		}

		// Display shutdown message
		log.info("Terminated");
		LogManager.shutdown();
	}
}