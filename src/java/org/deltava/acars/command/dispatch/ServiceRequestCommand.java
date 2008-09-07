// Copyright 2007, 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.dispatch;

import java.util.*;
import java.sql.Connection;

import org.deltava.beans.*;
import org.deltava.beans.acars.RoutePlan;
import org.deltava.beans.event.Event;
import org.deltava.beans.schedule.GeoPosition;

import org.deltava.dao.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.dispatch.*;

import org.gvagroup.acars.ACARSClientInfo;
import org.gvagroup.common.SharedData;

/**
 * An ACARS Command to handle Dispatch request messages.
 * @author Luke
 * @version 2.2
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
					boolean isOK = fr.hasAttribute(FlightReport.ATTR_CHARTER) || (fr.getDatabaseID(FlightReport.DBID_ASSIGN) > 0);
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
				routeValid |= (edao.getEvent(msg.getAirportD(), msg.getAirportA(), Event.NET_VATSIM) > 0);
				routeValid |= (edao.getEvent(msg.getAirportD(), msg.getAirportA(), Event.NET_IVAO) > 0);
				if (routeValid)
					log.info("Validated route " + msg.getAirportD() + " to " + msg.getAirportA() + " using Online Event");
			}
			
			// Set the flag
			msg.setRouteValid(routeValid);
		} catch (DAOException de) {
			log.error("Cannot validate route - " + de.getMessage(), de);
			AcknowledgeMessage errorMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			errorMsg.setEntry("error", "Cannot validate route");
			ctx.push(errorMsg, env.getConnectionID());
		} finally {
			ctx.release();
		}

		// Send to dispatchers
		int reqsSent = 0;
		Collection<ACARSConnection> cons = ctx.getACARSConnections("*");
		for (Iterator<ACARSConnection> i = cons.iterator(); i.hasNext(); ) {
			ACARSConnection ac = i.next();
			if (ac.getIsDispatch() && !ac.getUserBusy()) {
				GeoPosition gp = new GeoPosition(ac.getLocation());
				int distance = gp.distanceTo(msg);
				if (distance <= ac.getDispatchRange()) {
					reqsSent++;
					ctx.push(msg, ac.getID(), true);
				} else
					log.info("Dispatch service request not sent to " + ac.getUserID() + ", distance=" + distance);
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

		// If we have no dispatchers in range, then send back system routes
		log.info("No Dispatchers for " + c.getUserID() + ", doing Auto-Dispatch");
		Collection<RoutePlan> plans = new ArrayList<RoutePlan>();
		try {
			GetACARSRoute rdao = new GetACARSRoute(ctx.getConnection());
			plans.addAll(rdao.getRoutes(msg.getAirportD(), msg.getAirportA()));
		} catch (DAOException de) {
			log.error("Cannot load Dispatch routes - " + de.getMessage(), de);
			AcknowledgeMessage errorMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			errorMsg.setEntry("error", "Cannot load Dispatch routes");
			ctx.push(errorMsg, env.getConnectionID());
		} finally {
			ctx.release();
		}
		
		// Return back the routes
		if (!plans.isEmpty()) {
			RouteInfoMessage rmsg = new RouteInfoMessage(c.getUser(), msg.getID());
			for (RoutePlan rp : plans)
				rmsg.addPlan(rp);

			rmsg.setMessage("No Dispatchers available, loaded " + plans.size() + " Dispatch routes from database");
			ctx.push(rmsg, env.getConnectionID());
		} else {
			SystemTextMessage txtMsg = new SystemTextMessage();
			txtMsg.addMessage("No available Dispatchers within range, and no Dispatch routes found.");
			ctx.push(txtMsg, env.getConnectionID());
			ctx.push(new CancelMessage(env.getOwner()), env.getConnectionID());
		}
	}
}