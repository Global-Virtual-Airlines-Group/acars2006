// Copyright 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2015, 2017, 2019, 2021, 2023, 2024 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.ipc;

import java.util.*;
import java.io.Serializable;
import java.sql.Connection;

import org.apache.logging.log4j.*;

import org.deltava.beans.*;
import org.deltava.acars.beans.*;

import org.deltava.dao.*;

import org.deltava.util.cache.*;
import org.deltava.util.system.SystemData;

import org.gvagroup.common.*;
import org.gvagroup.pool.*;

/**
 * A daemon to listen for inter-process events.
 * @author Luke
 * @version 11.3
 * @since 1.0
 */

public class IPCDaemon implements Runnable {

	private static final Logger log = LogManager.getLogger(IPCDaemon.class);
	
	/**
	 * Returns the thread name.
	 * @return the tread name
	 */
	@Override
	public String toString() {
		return "ACARS IPC Daemon";
	}

	@Override
	public void run() {
		log.info("Starting");
		ACARSConnectionPool acPool = (ACARSConnectionPool) SharedData.get(SharedData.ACARS_POOL);
		ConnectionPool<Connection> cPool = SystemData.getJDBCPool();

		while (!Thread.currentThread().isInterrupted()) {
			try {
				EventDispatcher.waitForEvent();
				Connection con = null;
				Collection<SystemEvent> events = EventDispatcher.getEvents();
				try {
					con = cPool.getConnection();
					for (SystemEvent event : events) {
						Pilot usr = null; GetPilot pdao = new GetPilot(con);
						if (event instanceof UserEvent ue) {
							int userID = ue.getUserID();
							GetUserData uddao = new GetUserData(con);
							UserData ud = uddao.get(userID);
							usr = pdao.get(ud);
							if (usr == null) {
								log.warn("Unknown User ID - {}", Integer.valueOf(userID));
								continue;
							}
						}
						
						switch (event.getCode()) {
							case AIRLINE_RELOAD:
								log.warn("ACARS Reloading Airlines");
								GetAirline aldao = new GetAirline(con);
								SystemData.add("airlines", aldao.getAll());
								break;
								
							case AIRPORT_RELOAD:
								log.warn("ACARS Reloading Airports");
								GetAirport apdao = new GetAirport(con);
								SystemData.add("airports", apdao.getAll());
								break;
								
							case AIRPORT_RENAME:
								IDEvent ie = (IDEvent) event;
								if (ie.getData() == null) break;
								log.warn("ACARS renaming Airport {} to {}", ie.getData(), ie.getID());
								break;
								
							case AIRCRAFT_RENAME:
								ie = (IDEvent) event;
								if (ie.getData() == null) break;
								log.warn("ACARS renaming Aircraft {} to {}", ie.getData(), ie.getID());
								break;
								
							case TZ_RELOAD:
								log.warn("ACARS Reloading Time Zones");
								GetTimeZone tzdao = new GetTimeZone(con);
								tzdao.initAll();
								break;
								
							case USER_SUSPEND:
								if (usr == null) break;
								
								// Validate all of the connections
								log.warn("{} Suspended - Validating all Credentials", usr.getName());
								CacheManager.invalidate("Pilots", usr.cacheKey());
								for (ACARSConnection ac : acPool.getAll()) {
									if (ac.isAuthenticated()) {
										Pilot p = pdao.get(ac.getUserData());
										if (p.getStatus() != PilotStatus.ACTIVE) {
											log.warn("Disconnecting {}, Status = {}", p.getName(), p.getStatus().getDescription());
											ac.close();
											acPool.remove(ac);
										}
									}
								}
								
								break;
								
							case USER_INVALIDATE:
								if (usr == null) break;
								
								// Reload the user
								final int id = usr.getID();
								log.warn("ACARS Invalidated User {}", usr.getName());
								CacheManager.invalidate("Pilots", usr.cacheKey());
								Collection<ACARSConnection> cons = acPool.getAll(c -> c.isAuthenticated() && (c.getUser().getID() == id));
								for (ACARSConnection ac : cons) {
									log.info("Updated ACARS Connection record for {}", ac.getUser().getName());
									ac.setUser(usr);
								}
								
								break;
								
							case CACHE_STATS:
								Collection<CacheInfo> info = CacheManager.getCacheInfo(true);
								SharedData.addData(SharedData.ACARS_CACHEINFO, (Serializable) info);
								break;
								
							case CACHE_FLUSH:
								ie = (IDEvent) event;
								CacheManager.invalidate(ie.getID(), false);
								log.warn("ACARS flushing cache {}", ie.getID());
								break;
								
							default:
								log.warn("Unknown System Event ID - {}", event.getCode());
						}
					}
				} catch (Exception de) {
					log.atError().withThrowable(de).log(de.getMessage());
				} finally {
					cPool.release(con);
				}
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
		}
		
		EventDispatcher.unregister();
		log.info("Stopping");
	}
}