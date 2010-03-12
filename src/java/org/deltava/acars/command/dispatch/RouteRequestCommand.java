// Copyright 2007, 2008, 2009, 2010 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.dispatch;

import java.util.*;
import java.sql.Connection;

import org.deltava.beans.Pilot;
import org.deltava.beans.acars.*;
import org.deltava.beans.navdata.*;
import org.deltava.beans.schedule.*;
import org.deltava.beans.wx.METAR;

import org.deltava.comparators.RunwayComparator;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.AcknowledgeMessage;
import org.deltava.acars.message.dispatch.*;

import org.deltava.acars.command.*;

import org.deltava.dao.*;
import org.deltava.dao.wsdl.GetFARoutes;

import org.deltava.util.StringUtils;
import org.deltava.util.system.SystemData;

/**
 * An ACARS Command to load flight routes.
 * @author Luke
 * @version 3.0
 * @since 2.0
 */

public class RouteRequestCommand extends DispatchCommand {

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
		boolean doExternal = msg.getExternalRoutes() && ac.getIsDispatch()
			&& SystemData.getBoolean("schedule.flightaware.enabled");
		
		// Check if it's a US route
		boolean isUS = msg.getAirportD().getICAO().startsWith("K") || msg.getAirportD().getICAO().startsWith("P");
		isUS |= msg.getAirportA().getICAO().startsWith("K") || msg.getAirportA().getICAO().startsWith("P");
		if (doExternal && !isUS)
			log.warn(msg.getAirportD() + " - " + msg.getAirportA() + " is not a US route");

		try {
			RouteInfoMessage rmsg = new RouteInfoMessage(usr, msg.getID());
			Connection con = ctx.getConnection();
			
			// Load the routes
			GetACARSRoute rdao = new GetACARSRoute(con);
			Collection<DispatchRoute> plans = rdao.getRoutes(msg.getAirportD(), msg.getAirportA(), true);
			for (DispatchRoute rp : plans)
				rmsg.addPlan(rp);
			
			// If plans is empty and external routes are available, load them
			if (plans.isEmpty() && doExternal && isUS) {
				Collection<FlightRoute> eroutes = new ArrayList<FlightRoute>();
				GetCachedRoutes rcdao = new GetCachedRoutes(con);
				eroutes.addAll(rcdao.getRoutes(msg.getAirportD(), msg.getAirportA()));
				
				// Go to flightaware if nothing loaded
				if (eroutes.isEmpty()) {
					GetFARoutes fadao = new GetFARoutes();
					fadao.setUser(SystemData.get("schedule.flightaware.download.user"));
					fadao.setPassword(SystemData.get("schedule.flightaware.download.pwd"));
					Collection<? extends FlightRoute> faroutes = fadao.getRouteData(msg.getAirportD(), msg.getAirportA());
					if (!faroutes.isEmpty()) {
						eroutes.addAll(faroutes);
						SetCachedRoutes rcwdao = new SetCachedRoutes(con);
						rcwdao.write(faroutes);
					}
				}
				
				// Get the departure and arrival weather
				GetWeather wxdao = new GetWeather(con);
				METAR wxD = wxdao.getMETAR(msg.getAirportD().getICAO());
				METAR wxA = wxdao.getMETAR(msg.getAirportA().getICAO());
				
				// Get best runways
				GetACARSRunways rwdao = new GetACARSRunways(con);
				List<Runway> dRwys = rwdao.getPopularRunways(msg.getAirportD(), msg.getAirportA(), true);
				List<Runway> aRwys = rwdao.getPopularRunways(msg.getAirportD(), msg.getAirportA(), false);
				
				// Sort runways based on wind heading
				if (wxD != null) {
					RunwayComparator rcmp = new RunwayComparator(wxD.getWindDirection());
					Collections.sort(dRwys, Collections.reverseOrder(rcmp));
					dRwys.add(null);
				}
				if (wxA != null) {
					RunwayComparator rcmp = new RunwayComparator(wxA.getWindDirection());
					Collections.sort(aRwys, Collections.reverseOrder(rcmp));
					aRwys.add(null);
				}
				
				// Load the waypoints for each route
				GetNavRoute navdao = new GetNavRoute(con);
				for (FlightRoute rp : eroutes) {
					ExternalDispatchRoute dr = new ExternalDispatchRoute();
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
					
					// Load best SID
					if (!StringUtils.isEmpty(rp.getSID()) && (rp.getSID().contains("."))) {
						StringTokenizer tkns = new StringTokenizer(rp.getSID(), ".");
						String name = tkns.nextToken(); String wp = tkns.nextToken(); TerminalRoute sid = null;
						for (Iterator<Runway> ri = dRwys.iterator(); (sid == null) && ri.hasNext(); ) {
							Runway rwy = ri.next();
							sid = navdao.getBestRoute(rp.getAirportD(), TerminalRoute.SID, name, wp, rwy);
							if (sid != null) {
								dr.setSID(sid.getCode());
								for (NavigationDataBean nd : sid.getWaypoints())
									dr.addWaypoint(nd, sid.getCode());
							}
						}
					}
					
					// Load the route waypoints
					List<NavigationDataBean> points = navdao.getRouteWaypoints(rp.getRoute(), msg.getAirportD());
					for (NavigationDataBean nd : points)
						dr.addWaypoint(nd, nd.getAirway());
					
					// Load best STAR
					if (!StringUtils.isEmpty(rp.getSTAR()) && (rp.getSTAR().contains("."))) {
						StringTokenizer tkns = new StringTokenizer(rp.getSTAR(), ".");
						String name = tkns.nextToken(); String wp = tkns.nextToken(); TerminalRoute star = null;
						for (Iterator<Runway> ri = aRwys.iterator(); (star == null) && ri.hasNext(); ) {
							Runway rwy = ri.next();
							star = navdao.getBestRoute(rp.getAirportA(), TerminalRoute.STAR, name, wp, rwy);
							if (star != null) {
								dr.setSTAR(star.getCode());
								for (NavigationDataBean nd : star.getWaypoints())
									dr.addWaypoint(nd, star.getCode());
							}
						}
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