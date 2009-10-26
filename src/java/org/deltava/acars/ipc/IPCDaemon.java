// Copyright 2007, 2008, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.ipc;

import java.util.*;
import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.beans.*;
import org.deltava.acars.beans.*;

import org.deltava.dao.*;
import org.deltava.jdbc.*;

import org.deltava.util.system.SystemData;

import org.gvagroup.common.*;

/**
 * A daemon to listen for inter-process events.
 * @author Luke
 * @version 2.7
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
		
		ACARSConnectionPool acPool = null;
		ConnectionPool cPool = (ConnectionPool) SystemData.getObject(SystemData.JDBC_POOL);

		while (!Thread.currentThread().isInterrupted()) {
			try {
				EventDispatcher.waitForEvent();
				Connection con = null;
				Collection<SystemEvent> events = EventDispatcher.getEvents();
				try {
					for (Iterator<SystemEvent> i = events.iterator(); i.hasNext(); ) {
						SystemEvent event = i.next();
						con = cPool.getConnection();
						Pilot usr = null; GetPilot pdao = new GetPilot(con);
						if (event instanceof UserEvent) {
							int userID = ((UserEvent) event).getUserID();
							GetUserData uddao = new GetUserData(con);
							UserData ud = uddao.get(userID);
							usr = pdao.get(ud);
							if (usr == null) {
								log.warn("Unknown User ID - " + userID);
								continue;
							}
						}
						
						switch (event.getCode()) {
							case SystemEvent.AIRLINE_RELOAD:
								log.warn("ACARS Reloading Airlines");
								GetAirline aldao = new GetAirline(con);
								SystemData.add("airlines", aldao.getAll());
								break;
								
							case SystemEvent.AIRPORT_RELOAD:
								log.warn("ACARS Reloading Airports");
								GetAirport apdao = new GetAirport(con);
								SystemData.add("airports", apdao.getAll());
								break;
								
							case UserEvent.USER_SUSPEND:
								log.warn(usr.getName() + " Suspended - Validating all Credentials");
								
								// Validate all of the connections
								SetPilot.invalidate(usr.getID());
								acPool = (ACARSConnectionPool) SharedData.get(SharedData.ACARS_POOL);
								for (Iterator<ACARSConnection> ci = acPool.getAll().iterator(); ci.hasNext(); ) {
									ACARSConnection ac = ci.next();
									if (ac.isAuthenticated()) {
										Pilot p = pdao.get(ac.getUserData());
										if (p.getStatus() != Pilot.ACTIVE) {
											log.warn("Disconnecting " + p.getName() + ", Status = " + p.getStatusName());
											acPool.remove(ac);
										}
									}
								}
								
								break;
								
							case UserEvent.USER_INVALIDATE:
								log.warn("Invalidated User " + usr.getName());
								
								// Reload the user
								SetPilot.invalidate(usr.getID());
								acPool = (ACARSConnectionPool) SharedData.get(SharedData.ACARS_POOL);
								ACARSConnection ac = acPool.get(usr.getPilotCode());
								if (ac != null) {
									log.info("Updated ACARS Connection record for " + ac.getUser().getName());
									ac.setUser(usr);
								}
								
								break;
								
							default:
								log.warn("Unknown System Event ID - " + event.getCode());
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