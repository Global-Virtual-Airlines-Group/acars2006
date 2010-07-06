// Copyright 2007, 2008, 2009, 2010 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.dispatch;

import java.util.*;
import java.sql.Connection;

import org.deltava.beans.*;
import org.deltava.beans.acars.*;
import org.deltava.beans.flight.*;
import org.deltava.beans.navdata.*;
import org.deltava.beans.schedule.*;
import org.deltava.beans.wx.METAR;

import org.deltava.comparators.RunwayComparator;

import org.deltava.dao.*;

import org.deltava.util.system.SystemData;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.dispatch.*;

import org.gvagroup.acars.ACARSClientInfo;
import org.gvagroup.common.SharedData;

/**
 * An ACARS Command to handle Dispatch request messages.
 * @author Luke
 * @version 3.1
 * @since 2.0
 */

public class ServiceRequestCommand extends DispatchCommand {

	/**
	 * Initializes the Command.
	 */
	public ServiceRequestCommand() {
		super(ServiceRequestCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the inbound message
		RequestMessage msg = (RequestMessage) env.getMessage();
		
		// Check minimum build number
		ACARSConnection c = ctx.getACARSConnection();
		UserData ud = c.getUserData();
		ACARSClientInfo cInfo = (ACARSClientInfo) SharedData.get(SharedData.ACARS_CLIENT_BUILDS);
		if (cInfo.getNoDispatchBuilds().contains(Integer.valueOf(c.getClientVersion()))) {
			log.warn(c.getUser().getName() + " requesting Dispatch service using invalid build " + c.getClientVersion());
			
			// Send response
			SystemTextMessage txtMsg = new SystemTextMessage();
			txtMsg.addMessage("ACARS Client Build " + c.getClientVersion() + " cannot request Dispatch service");
			ctx.push(txtMsg, env.getConnectionID());
			return;
		}
		
		// Validate that the route is valid
		Collection<PopulatedRoute> plans = new ArrayList<PopulatedRoute>();
		try {
			Connection con = ctx.getConnection();
			
			// Check the schedule
			GetSchedule sdao = new GetSchedule(con);
			boolean routeValid = (sdao.getFlightTime(msg.getAirportD(), msg.getAirportA(), ud.getDB()) > 0);
			
			// If we're not valid, check for a draft flight assignment
			if (!routeValid) {
				GetFlightReports prdao = new GetFlightReports(con);
				Collection<FlightReport> pireps = prdao.getDraftReports(ud.getID(), msg.getAirportD(), msg.getAirportA(), ud.getDB());
				for (Iterator<FlightReport> i = pireps.iterator(); i.hasNext() && !routeValid; ) {
					FlightReport fr = i.next();
					boolean isOK = fr.hasAttribute(FlightReport.ATTR_CHARTER) || (fr.getDatabaseID(DatabaseID.ASSIGN) > 0);
					isOK &= msg.getAirportD().equals(fr.getAirportD());
					isOK &= msg.getAirportA().equals(fr.getAirportA());
					if (routeValid) {
						log.info("Validated route " + msg.getAirportD() + " to " + msg.getAirportA() + " using draft PIREP");
						routeValid = true;
					}
				}
			}
			
			// If we're still not valid, check for an event
			if (!routeValid) {
				GetEvent edao = new GetEvent(con); 
				routeValid |= (edao.getEvent(msg.getAirportD(), msg.getAirportA(), OnlineNetwork.VATSIM) > 0);
				routeValid |= (edao.getEvent(msg.getAirportD(), msg.getAirportA(), OnlineNetwork.IVAO) > 0);
				if (routeValid)
					log.info("Validated route " + msg.getAirportD() + " to " + msg.getAirportA() + " using Online Event");
			}
			
			// Set the flag
			msg.setRouteValid(routeValid);
			
			// Load existing plans
			GetACARSRoute rdao = new GetACARSRoute(con);
			plans.addAll(rdao.getRoutes(msg.getAirportD(), msg.getAirportA(), true));
			
			// If we don't have any plans but have cached routes, use them
			if (plans.isEmpty()) {
				GetWeather wxdao = new GetWeather(con);
				GetACARSRunways rwdao = new GetACARSRunways(con);
				
				// Get the departure runways based on weather
				METAR wxD = wxdao.getMETAR(msg.getAirportD().getICAO());
				List<Runway> rwyD = rwdao.getPopularRunways(msg.getAirportD(), msg.getAirportA(), true);
				if (wxD != null) {
					RunwayComparator rcmp = new RunwayComparator(wxD.getWindDirection());
					Collections.sort(rwyD, rcmp);
				}
				
				// Convert runways to strings
				List<String> rD = new ArrayList<String>();
				for (Runway r : rwyD)
					rD.add("RW" + r.getName());
				
				// Get the arrival runways based on weather
				METAR wxA = wxdao.getMETAR(msg.getAirportA().getICAO());
				List<Runway> rwyA = rwdao.getPopularRunways(msg.getAirportD(), msg.getAirportA(), false);
				if (wxA != null) {
					RunwayComparator rcmp = new RunwayComparator(wxA.getWindDirection());
					Collections.sort(rwyA, rcmp);
				}
				
				// Convert runways to strings
				List<String> rA = new ArrayList<String>();
				for (Runway r : rwyA)
					rA.add("RW" + r.getName());
				
				// Populate the routes
				GetNavRoute navdao = new GetNavRoute(con);
				GetCachedRoutes rcdao = new GetCachedRoutes(con);
				Collection<? extends FlightRoute> faRoutes = rcdao.getRoutes(msg.getAirportD(), msg.getAirportA());
				for (FlightRoute fr : faRoutes) {
					PopulatedRoute pr = navdao.populate(fr, rD, rA);
					ExternalDispatchRoute edr = new ExternalDispatchRoute(pr);
					edr.setAirline(SystemData.getAirline(ud.getAirlineCode()));
					edr.setAirportL(msg.getAirportL());
					plans.add(edr);
				}
			}
		} catch (DAOException de) {
			log.error("Cannot validate/load route - " + de.getMessage(), de);
			AcknowledgeMessage errorMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			errorMsg.setEntry("error", "Cannot validate route");
			ctx.push(errorMsg, env.getConnectionID());
		} finally {
			ctx.release();
		}

		// Send to dispatchers if not in auto dispatch mode
		int reqsSent = 0; int outOfRange = 0;
		Collection<ACARSConnection> cons = ctx.getACARSConnectionPool().getAll();
		if (!msg.isAutoDispatch() || plans.isEmpty()) {
			for (Iterator<ACARSConnection> i = cons.iterator(); i.hasNext(); ) {
				ACARSConnection ac = i.next();
				if (ac.getIsDispatch() && !ac.getUserBusy() && !ac.getUserHidden()) {
					GeoPosition gp = new GeoPosition(ac.getLocation());
					int distance = gp.distanceTo(msg);
					if (distance <= ac.getDispatchRange()) {
						reqsSent++;
						ctx.push(msg, ac.getID(), true);
					} else {
						outOfRange++;
						log.info("Dispatch service request not sent to " + ac.getUserID() + ", distance=" + distance);
					}
				}
			}
		}
		
		// If we sent to a dispatcher, send a system text message back
		if (reqsSent > 0) {
			SystemTextMessage txtMsg = new SystemTextMessage();
			txtMsg.addMessage("Dispatch Request sent to " + reqsSent + " Dispatcher(s)");
			if (!msg.isRouteValid())
				txtMsg.addMessage("You are requesting an Invalid Route, and are unlikely to receive Service!");
			
			ctx.push(txtMsg, env.getConnectionID());
			return;
		}

		// Return back the routes
		if (!plans.isEmpty()) {
			RouteInfoMessage rmsg = new RouteInfoMessage(c.getUser(), msg.getID());
			for (PopulatedRoute rp : plans)
				rmsg.addPlan(rp);
			
			// Build message
			StringBuilder buf = new StringBuilder("No Dispatchers available");
			if (outOfRange > 0)
				buf.append(" (" + outOfRange + " out of range)");

			buf.append(", loaded " + plans.size() + " Dispatch routes from database");
			rmsg.setMessage(buf.toString());
			ctx.push(rmsg, env.getConnectionID());
		} else {
			SystemTextMessage txtMsg = new SystemTextMessage();
			if (outOfRange > 0)
				txtMsg.addMessage(outOfRange + " Dispatchers online, but out of range");
			txtMsg.addMessage("No available Dispatchers within range, and no Dispatch routes found.");
			ctx.push(txtMsg, env.getConnectionID());
			ctx.push(new CancelMessage(env.getOwner()), env.getConnectionID());
		}
	}
}