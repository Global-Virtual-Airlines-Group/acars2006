// Copyright 2010 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.dispatch;

import java.util.*;
import java.sql.Connection;

import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.message.AcknowledgeMessage;
import org.deltava.acars.message.dispatch.RoutePlotMessage;

import org.deltava.acars.command.*;

import org.deltava.beans.navdata.*;
import org.deltava.beans.schedule.*;
import org.deltava.beans.wx.METAR;

import org.deltava.comparators.RunwayComparator;

import org.deltava.dao.*;
import org.deltava.util.StringUtils;

/**
 * An ACARS Command to plot a flight route.
 * @author Luke
 * @version 5.1
 * @since 3.0
 */

public class RoutePlotCommand extends DispatchCommand {

	/**
	 * Initializes the Command.
	 */
	public RoutePlotCommand() {
		super(RoutePlotCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Cast the message
		RoutePlotMessage msg = (RoutePlotMessage) env.getMessage();
		
		// Split the route
		List<String> wps = StringUtils.split(msg.getRoute(), " ");
		wps.remove(msg.getAirportD().getICAO());
		wps.remove(msg.getAirportA().getICAO());

		try {
			Connection con = ctx.getConnection();
			
			// Get the departure and arrival weather
			GetWeather wxdao = new GetWeather(con);
			METAR wxD = wxdao.getMETAR(msg.getAirportD().getICAO());
			METAR wxA = wxdao.getMETAR(msg.getAirportA().getICAO());
			
			// Get best runways
			GetACARSRunways rwdao = new GetACARSRunways(con);
			List<Runway> dRwys = rwdao.getPopularRunways(msg.getAirportD(), msg.getAirportA(), true);
			List<Runway> aRwys = rwdao.getPopularRunways(msg.getAirportD(), msg.getAirportA(), false);
			
			// Sort runways based on wind heading
			if ((wxD != null) && (wxD.getWindSpeed() > 0)) {
				Collections.sort(dRwys, new RunwayComparator(wxD.getWindDirection()).reverse());
				dRwys.add(null);
			}
			if ((wxA != null) && (wxA.getWindSpeed() > 0)) {
				Collections.sort(aRwys, new RunwayComparator(wxA.getWindDirection()).reverse());
				aRwys.add(null);
			}
			
			// Create the response
			PopulatedRoute rt = new PopulatedRoute();
			rt.setAirportD(msg.getAirportD());
			rt.setAirportA(msg.getAirportA());
			rt.setCreatedOn(new Date());
			rt.setComments("User-Requested Route");
			rt.setRoute(msg.getRoute());
			
			// Load the waypoints for the route
			GetNavRoute navdao = new GetNavRoute(con);

			// Load best SID
			if (!StringUtils.isEmpty(msg.getSID()) && (msg.getSID().contains("."))) {
				StringTokenizer tkns = new StringTokenizer(msg.getSID(), ".");
				String name = TerminalRoute.makeGeneric(tkns.nextToken()); String wp = tkns.nextToken(); TerminalRoute sid = null;
				for (Iterator<Runway> ri = dRwys.iterator(); (sid == null) && ri.hasNext(); ) {
					Runway rwy = ri.next();
					sid = navdao.getBestRoute(msg.getAirportD(), TerminalRoute.Type.SID, name, wp, rwy);
					if (sid != null) {
						msg.setSID(sid.getCode());
						rt.setSID(sid.getCode());
						for (NavigationDataBean nd : sid.getWaypoints())
							rt.addWaypoint(nd, sid.getCode());
					}
				}
			}
			
			// Load the route waypoints
			List<NavigationDataBean> points = navdao.getRouteWaypoints(msg.getRoute(), msg.getAirportD());
			for (NavigationDataBean nd : points)
				rt.addWaypoint(nd, nd.getAirway());
			
			// Load best STAR
			if (!StringUtils.isEmpty(msg.getSTAR()) && (msg.getSTAR().contains("."))) {
				StringTokenizer tkns = new StringTokenizer(msg.getSTAR(), ".");
				String name = TerminalRoute.makeGeneric(tkns.nextToken()); String wp = tkns.nextToken(); TerminalRoute star = null;
				for (Iterator<Runway> ri = aRwys.iterator(); (star == null) && ri.hasNext(); ) {
					Runway rwy = ri.next();
					star = navdao.getBestRoute(msg.getAirportA(), TerminalRoute.Type.STAR, name, wp, rwy);
					if (star != null) {
						msg.setSTAR(star.getCode());
						rt.setSTAR(star.getCode());
						for (NavigationDataBean nd : star.getWaypoints())
							rt.addWaypoint(nd, star.getCode());
					}
				}
			}
			
			msg.setResults(rt);
		} catch (DAOException de) {
			log.error("Cannot plot route - " + de.getMessage(), de);
			AcknowledgeMessage errorMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			errorMsg.setEntry("error", "Cannot plot route");
			ctx.push(errorMsg, env.getConnectionID());
		} finally {
			ctx.release();
		}
		
		// Push the message back
		ctx.push(msg, ctx.getACARSConnection().getID());
	}
}