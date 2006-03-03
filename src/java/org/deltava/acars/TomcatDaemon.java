// Copyright 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars;

import java.util.*;

import org.apache.log4j.Logger;

import org.deltava.acars.workers.*;
import org.deltava.beans.acars.ACARSWorkerInfo;

import org.deltava.util.ThreadUtils;

/**
 * An ACARS Server daemon to be run in a Tomcat instance.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class TomcatDaemon extends ServerDaemon implements Runnable, ACARSWorkerInfo {

	public void run() {

		// Initialize the logger, connection pool and server tasks
		log = Logger.getLogger("ACARSDaemon");
		initACARSConnectionPool();
		initTasks();

		// Log the start of the loop
		log.info("Started");
		ThreadUtils.sleep(5000);

		// Start looping
		while (!Thread.currentThread().isInterrupted()) {
			try {
				// Check all of the threads
				for (Iterator<Worker> i = _threads.keySet().iterator(); i.hasNext();) {
					Worker w = i.next();
					WorkerStatus ws = w.getStatus();

					// Get the thread status
					Thread t = _threads.get(w);
					ws.setAlive(t.isAlive());
					if (!t.isAlive()) {
						log.warn(t.getName() + " not running, restarting");

						// Restart the worker thread
						Thread wt = new Thread(_workers, w, w.getName());
						_threads.put(w, wt);
						wt.start();
					} else if (ws.getExecutionTime() > MAX_EXEC) {
						log.warn(t.getName() + " stuck for " + ws.getExecutionTime() + "ms, restarting");
						log.warn("Last activity - " + ws.getMessage());

						// Kill the worker thread
						ThreadUtils.kill(t, 1000);

						// Restart the worker thread
						Thread wt = new Thread(_workers, w, w.getName());
						_threads.put(w, wt);
						wt.start();
					}
				}

				// Go to sleep for a while - if interrupted, shut down the loop
				Thread.sleep(10000);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				log.error("Error restarting worker - " + e.getMessage(), e);
			}
		}

		// Try to close the workers down
		_workers.interrupt();
		for (Iterator<Worker> i = _threads.keySet().iterator(); i.hasNext();) {
			Worker w = i.next();

			// Wait for the thread to die if it hasn't yet
			Thread t = _threads.get(w);
			ThreadUtils.kill(t, 500);

			// Close the thread
			log.debug("Stopping " + w.getName());
			w.close();
		}

		// Display shutdown message
		log.info("Terminated");
	}

	/**
	 * Returns worker status to the web application.
	 * @see ACARSWorkerInfo#getWorkers()
	 */
	public Collection<WorkerStatus> getWorkers() {
		Collection<WorkerStatus> results = new TreeSet<WorkerStatus>();
		for (Iterator<Worker> i = _threads.keySet().iterator(); i.hasNext();) {
			Worker w = i.next();
			results.add(w.getStatus());
		}

		return results;
	}
}