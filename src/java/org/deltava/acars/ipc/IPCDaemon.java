// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.ipc;

import java.util.*;
import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.beans.Pilot;
import org.deltava.acars.beans.*;

import org.deltava.dao.*;
import org.deltava.jdbc.*;

import org.deltava.util.system.SystemData;

import org.gvagroup.common.*;

/**
 * A daemon to listen for inter-process events.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class IPCDaemon implements Runnable {

	private static final Logger log = Logger.getLogger(IPCDaemon.class);
	
	/**
	 * Returns the thread name.
	 * @return the tread name
	 */
	public String toString() {
		return "ACARS IPC Daemon";
	}

	/**
	 * Executes the thread.
	 */
	public void run() {
		log.info("Starting");
		ConnectionPool cPool = (ConnectionPool) SystemData.getObject(SystemData.JDBC_POOL);

		while (!Thread.currentThread().isInterrupted()) {
			try {
				EventDispatcher.waitForEvent();
				Collection<SystemEvent> events = EventDispatcher.getEvents();
				Connection con = null;
				try {
					for (Iterator<SystemEvent> i = events.iterator(); i.hasNext(); ) {
						SystemEvent event = i.next();
						if (event == SystemEvent.AIRLINE_RELOAD) {
							log.warn("ACARS Reloading Airlines");
							con = cPool.getConnection(true);
							GetAirline adao = new GetAirline(con);
							SystemData.add("airlines", adao.getAll());
						} else if (event == SystemEvent.AIRPORT_RELOAD) {
							log.warn("ACARS Reloading Airports");
							con = cPool.getConnection(true);
							GetAirport adao = new GetAirport(con);
							SystemData.add("airports", adao.getAll());
						} else if (event == SystemEvent.USER_SUSPEND) {
							log.warn("User Suspended - Validating all Credentials");
							ACARSConnectionPool acPool = (ACARSConnectionPool) SharedData.get(SharedData.ACARS_POOL);
							con = cPool.getConnection(true);
							
							// Validate all of the connections
							GetPilot pdao = new GetPilot(con);
							for (Iterator<ACARSConnection> ci = acPool.get("*").iterator(); ci.hasNext(); ) {
								ACARSConnection ac = ci.next();
								if (ac.isAuthenticated()) {
									Pilot p = pdao.get(ac.getUserData());
									if (p.getStatus() != Pilot.ACTIVE) {
										log.warn("Disconnecting " + p.getName() + ", Status = " + p.getStatusName());
										ac.close();
									}
								}
							}
						}
					}
				} catch (DAOException de) {
					log.error(de.getMessage(), de);
				} finally {
					cPool.release(con);
				}
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
		}
		
		log.info("Stopping");
	}
}