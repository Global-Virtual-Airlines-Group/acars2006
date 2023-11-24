// Copyright 2005, 2006, 2007, 2010, 2011, 2016, 2020, 2022, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.logging.log4j.*;

import org.deltava.acars.workers.*;
import org.deltava.util.ThreadUtils;

import org.gvagroup.ipc.*;

/**
 * An ACARS Server daemon to be run in a Tomcat instance.
 * @author Luke
 * @version 11.1
 * @since 1.0
 */

public class TomcatDaemon extends ServerDaemon implements Runnable, PoolWorkerInfo, java.io.Serializable {

	@Override
	public void run() {

		// Initialize the logger, connection pool and server tasks
		log = LogManager.getLogger("ACARSDaemon");
		initACARSConnectionPool();
		initTasks();

		// Log the start of the loop
		log.info("Started");
		ThreadUtils.sleep(5000);

		// Start looping
		while (!Thread.currentThread().isInterrupted()) {
			try {
				// Check all of the threads
				Map<Thread, Worker> threads = new HashMap<Thread,Worker >(_threads);
				for (Iterator<Map.Entry<Thread, Worker>> i = threads.entrySet().iterator(); i.hasNext();) {
					Map.Entry<Thread, Worker> we = i.next();
					Thread t = we.getKey();
					Worker w = we.getValue();
					List<WorkerStatus> wsl = w.getStatus();
					WorkerStatus ws = wsl.get(0);

					// Get the thread status
					ws.setAlive(t.isAlive());
					if (!t.isAlive()) {
						log.warn("{} not running, restarting", t.getName());
						_threads.remove(t);

						// Restart the worker thread
						Thread wt = new Thread(_workers, w, w.getName());
						wt.setUncaughtExceptionHandler(this);
						_threads.put(wt, w);
						wt.start();
					} else if (ws.getExecutionTime() > MAX_EXEC) {
						log.warn("{} stuck for {}ms, restarting", t.getName(), Long.valueOf(ws.getExecutionTime()));
						log.warn("Last activity - {}", ws.getMessage());

						// Kill the worker thread
						ThreadUtils.kill(t, 1250);
						_threads.remove(t);

						// Restart the worker thread
						Thread wt = new Thread(_workers, w, w.getName());
						wt.setUncaughtExceptionHandler(this);
						_threads.put(wt, w);
						wt.start();
					}
				}

				// Go to sleep for a while - if interrupted, shut down the loop
				Thread.sleep(10000);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				log.atError().withThrowable(e).log("Error restarting worker - {}", e.getMessage());
			}
		}
		
		// Remove uncaught exception handler if we're shutting threads down
		_threads.keySet().forEach(wt -> { wt.setUncaughtExceptionHandler(null); wt.interrupt(); });

		// Try to close the workers down
		ThreadUtils.sleep(250);
		_threads.values().forEach(w -> { log.debug("Stopping {}", w.getName()); w.close(); });

		// Display shutdown message
		ThreadUtils.kill(_threads.keySet(), 1500);
		log.info("Terminated");
	}

	@Override
	public Collection<WorkerStatus> getWorkers() {
		return _threads.values().stream().map(Worker::getStatus).flatMap(Collection::stream).collect(Collectors.toCollection(TreeSet::new));
	}
	
	@Override
	public String toString() {
		return "ACARS Daemon";
	}
}