// Copyright 2007, 2008, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.dispatch;

import java.util.*;
import java.sql.Connection;

import org.deltava.beans.Pilot;
import org.deltava.beans.acars.DispatchRoute;
import org.deltava.beans.navdata.*;
import org.deltava.beans.schedule.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.AcknowledgeMessage;
import org.deltava.acars.message.dispatch.*;

import org.deltava.acars.command.*;

import org.deltava.dao.*;
import org.deltava.dao.wsdl.GetFARoutes;

import org.deltava.util.system.SystemData;

/**
 * An ACARS Command to load flight routes.
 * @author Luke
 * @version 2.3
 * @since 2.0
 */

public class RouteRequestCommand extends DispatchCommand {

	public class PopulatedFARoute extends DispatchRoute implements ExternalFlightRoute {
		
		private String _source;
		
		public String getSource() {
			return _source;
		}
		
		public void setSource(String src) {
			_source = src;
		}
	}
	
	/**
	 * Initializes the Command.
	 */
	public RouteRequestCommand() {
		super(RouteRequestCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the inbound message and the owner
		Pilot usr = env.getOwner();
		ACARSConnection ac = ctx.getACARSConnection();
		RouteRequestMessage msg = (RouteRequestMessage) env.getMessage();
		boolean doExternal = msg.getExternalRoutes() && usr.isInRole("Route") && ac.getIsDispatch()
			&& SystemData.getBoolean("schedule.flightaware.enabled");

		try {
			RouteInfoMessage rmsg = new RouteInfoMessage(usr, msg.getID());
			Connection con = ctx.getConnection();
			
			// Load the routes
			GetACARSRoute rdao = new GetACARSRoute(con);
			Collection<DispatchRoute> plans = rdao.getRoutes(msg.getAirportD(), msg.getAirportA(), true);
			for (DispatchRoute rp : plans)
				rmsg.addPlan(rp);
			
			// If plans is empty and external routes are available, load them
			if (plans.isEmpty() && doExternal) {
				GetFARoutes fadao = new GetFARoutes();
				fadao.setUser(SystemData.get("schedule.flightaware.download.user"));
				fadao.setPassword(SystemData.get("schedule.flightaware.download.pwd"));
				Collection<FlightRoute> eroutes = fadao.getRouteData(msg.getAirportD(), msg.getAirportA());
				
				// Load the waypoints for each route
				GetNavRoute navdao = new GetNavRoute(con);
				for (FlightRoute rp : eroutes) {
					PopulatedFARoute dr = new PopulatedFARoute();
					dr.setAirportD(msg.getAirportD());
					dr.setAirportA(msg.getAirportA());
					dr.setAirline(SystemData.getAirline(ac.getUserData().getAirlineCode()));
					dr.setComments(rp.getComments());
					dr.setCreatedOn(rp.getCreatedOn());
					dr.setDispatchBuild(ac.getClientVersion());
					dr.setCruiseAltitude(rp.getCruiseAltitude());
					dr.setRoute(rp.getRoute());
					if (rp instanceof ExternalFlightRoute)
						dr.setSource(((ExternalFlightRoute) rp).getSource());
					
					// Load the SID waypoints
					TerminalRoute sid = navdao.getRoute(rp.getSID());
					if (sid != null) {
						dr.setSID(rp.getSID());
						for (NavigationDataBean nd : sid.getWaypoints())
							dr.addWaypoint(nd, sid.getCode());
					}
					
					// Load the route waypoints
					List<NavigationDataBean> points = navdao.getRouteWaypoints(rp.getRoute(), msg.getAirportD());
					for (NavigationDataBean nd : points)
						dr.addWaypoint(nd, nd.getAirway());
					
					// Load the STAR waypoints
					TerminalRoute star = navdao.getRoute(rp.getSTAR());
					if (star != null) {
						dr.setSTAR(rp.getSTAR());
						for (NavigationDataBean nd : star.getWaypoints())
							dr.addWaypoint(nd, star.getCode());
					}
					
					// Save the converted route
					rmsg.addPlan(dr);
				}
			}
			
			// Send the response
			if (!ac.getIsDispatch())
				rmsg.setMessage("Loaded " + plans.size() + " Dispatch routes from database");
			ctx.push(rmsg, env.getConnectionID());
		} catch (DAOException de) {
			log.error("Cannot load route data - " + de.getMessage(), de);
			AcknowledgeMessage errorMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			errorMsg.setEntry("error", "Cannot load route data");
			ctx.push(errorMsg, env.getConnectionID());
		} finally {
			ctx.release();
		}
	}
	
	/**
	 * Returns the maximum execution time of this command before a warning is issued.
	 * @return the maximum execution time in milliseconds
	 */
	public final int getMaxExecTime() {
		return 2500;
	}
}