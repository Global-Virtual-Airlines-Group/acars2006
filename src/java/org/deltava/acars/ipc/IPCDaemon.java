// Copyright 2007, 2008, 2009, 2010, 2011, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.ipc;

import java.util.*;
import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.beans.*;
import org.deltava.beans.mvs.*;
import org.deltava.acars.beans.*;

import org.deltava.dao.*;

import org.deltava.util.CollectionUtils;
import org.deltava.util.cache.CacheManager;
import org.deltava.util.system.SystemData;

import org.gvagroup.common.*;
import org.gvagroup.jdbc.*;

/**
 * A daemon to listen for inter-process events.
 * @author Luke
 * @version 5.0
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
	@Override
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
								
							case TZ_RELOAD:
								log.warn("ACARS Reloading Time Zones");
								GetTimeZone tzdao = new GetTimeZone(con);
								tzdao.initAll();
								break;
								
							case MVS_RELOAD:
								log.warn("Reloading persistent Voice channels");
								GetMVSChannel chdao = new GetMVSChannel(con);
								Map<String, Channel> channels = CollectionUtils.createMap(chdao.getAll(), "name");
								VoiceChannels vc = VoiceChannels.getInstance();
								
								// Update existing channels
								for (Map.Entry<String, Channel> me : channels.entrySet()) {
									PopulatedChannel pc = vc.get(me.getKey());
									if (pc == null) {
										log.warn("Added new MVS channel " + me.getKey());
										vc.add(null, me.getValue());
									} else if (pc.size() == 0) {
										vc.remove(me.getKey());
										vc.add(null, me.getValue());
										log.warn("Updated MVS channel " + me.getKey());
									} else
										log.warn("MVS channel " + me.getKey() + " not empty!");
								}
								
								// Delete old channels
								for (PopulatedChannel pc : vc.getChannels()) {
									Channel c = pc.getChannel();
									if (c.getIsTemporary())
										continue;
									
									if (!channels.containsKey(c.getName())) {
										log.warn("Removing MVS channel " + c.getName());
										vc.remove(c.getName());
									}
								}
								
								break;
								
							case USER_SUSPEND:
								if (usr == null)
									break;
								
								// Validate all of the connections
								log.warn(usr.getName() + " Suspended - Validating all Credentials");
								CacheManager.invalidate("Pilots", usr.cacheKey());
								acPool = (ACARSConnectionPool) SharedData.get(SharedData.ACARS_POOL);
								for (Iterator<ACARSConnection> ci = acPool.getAll().iterator(); ci.hasNext(); ) {
									ACARSConnection ac = ci.next();
									if (ac.isAuthenticated()) {
										Pilot p = pdao.get(ac.getUserData());
										if (p.getStatus() != Pilot.ACTIVE) {
											log.warn("Disconnecting " + p.getName() + ", Status = " + p.getStatusName());
											ac.close();
											acPool.remove(ac);
										}
									}
								}
								
								break;
								
							case USER_INVALIDATE:
								if (usr == null)
									break;
								
								// Reload the user
								log.warn("Invalidated User " + usr.getName());
								CacheManager.invalidate("Pilots", usr.cacheKey());
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
				} catch (Exception de) {
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