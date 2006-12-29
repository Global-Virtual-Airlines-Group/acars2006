// Copyright 2004, 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars;

import java.util.*;

import org.apache.log4j.LogManager;

import org.deltava.beans.system.VersionInfo;

import org.deltava.acars.workers.*;

import org.deltava.util.ThreadUtils;
import org.deltava.util.system.SystemData;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class StandaloneDaemon extends ServerDaemon {

	public static void main(String[] args) {

		// Startup message
		System.out.println("ACARS " + VersionInfo.APPNAME);
		System.out.println(VersionInfo.TXT_COPYRIGHT);

		// Initialize the logger
		initLog(StandaloneDaemon.class);

		// Start everything up
		SystemData.init();
		initAuthenticator();
		initConnectionPool();
		initAirports();

		// Init the connection pool and server tasks
		initACARSConnectionPool();
		initTasks();

		// Log the start of the loop
		log.info("Started");

		// Start looping
		while (!Thread.currentThread().isInterrupted()) {
			// Go to sleep for a while - if interrupted, shut down the loop
			try {
				Thread.sleep(45000);

				// Check all of the threads
				for (Iterator<Thread> i = _threads.keySet().iterator(); i.hasNext();) {
					Thread t = i.next();
					Worker w = _threads.get(t);
					List<WorkerStatus> wsl = w.getStatus();
					WorkerStatus ws = wsl.get(0);

					// Get the thread status
					if (!t.isAlive()) {
						log.warn(t.getName() + " not running, restarting");

						// Restart the worker thread
						Thread wt = new Thread(_workers, w, w.getName());
						_threads.put(wt, w);
						wt.start();
					} else if (ws.getExecutionTime() > MAX_EXEC) {
						log.warn(t.getName() + " stuck for " + ws.getExecutionTime() + "ms, restarting");
						log.warn("Last activity - " + ws.getMessage());

						// Kill the worker thread
						ThreadUtils.kill(t, 1000);

						// Restart the worker thread
						Thread wt = new Thread(_workers, w, w.getName());
						_threads.put(wt, w);
						wt.start();
					}
				}
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
		}

		// Try to close the workers down
		_workers.interrupt();
		for (Iterator<Thread> i = _threads.keySet().iterator(); i.hasNext();) {
			Thread t = i.next();
			Worker w = _threads.get(t);

			// Wait for the thread to die if it hasn't yet
			ThreadUtils.kill(t, 500);

			// Close the thread
			log.info("Stopping " + w.getName());
			w.close();
		}

		// Display shutdown message
		log.info("Terminated");
		LogManager.shutdown();
	}
}