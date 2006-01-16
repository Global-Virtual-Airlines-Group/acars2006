// Copyright 2005 Luke J. Kolin. All Rights Reserved.
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

		// Initialize the logger
		log = Logger.getLogger("ACARSDaemon");

		// Init the connection pool
		initACARSConnectionPool();

		// Init the server tasks
		initTasks();

		// Log the start of the loop
		log.info("Started");
		ThreadUtils.sleep(10000);

		// Start looping
		while (!Thread.currentThread().isInterrupted()) {
			// Go to sleep for a while - if interrupted, shut down the loop
			try {
				// Check all of the threads
				for (Iterator<Worker> i = _threads.keySet().iterator(); i.hasNext(); ) {
					Worker w =  i.next();

					// Get the thread status
					Thread t = _threads.get(w);
					w.getStatus().setAlive(t.isAlive());
					if (!t.isAlive()) {
						log.warn(t.getName() + " not running, restarting");
						
						// Restart the worker thread
						Thread wt = new Thread(_workers, w, w.getName());
						_threads.put(w, wt);
						wt.start();
					}
				}
				
				Thread.sleep(45000);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				log.error("Error restarting worker - " + e.getMessage(), e);
			}
		}

		// Interrupt the threads
		_workers.interrupt();

		// Try to close the workers down
		for (Iterator<Worker> i = _threads.keySet().iterator(); i.hasNext();) {
			Worker w = i.next();

			// Wait for the thread to die if it hasn't yet
			Thread t = _threads.get(w);
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
	
	public Collection<WorkerStatus> getWorkers() {
		Set<WorkerStatus> results = new TreeSet<WorkerStatus>();
		for (Iterator<Worker> i = _threads.keySet().iterator(); i.hasNext(); ) {
			Worker w = i.next();
			results.add(w.getStatus());
		}
		
		return results;
	}
}