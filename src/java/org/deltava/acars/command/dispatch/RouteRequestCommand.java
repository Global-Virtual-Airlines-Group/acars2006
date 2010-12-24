// Copyright 2007, 2008, 2009, 2010 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.dispatch;

import java.util.*;
import java.sql.Connection;

import org.deltava.beans.Pilot;
import org.deltava.beans.acars.*;
import org.deltava.beans.schedule.*;
import org.deltava.beans.system.AirlineInformation;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.AcknowledgeMessage;
import org.deltava.acars.message.dispatch.*;

import org.deltava.acars.command.*;

import org.deltava.dao.*;

import org.deltava.util.system.SystemData;

/**
 * An ACARS Command to load flight routes.
 * @author Luke
 * @version 3.4
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
		boolean doExternal = msg.getExternalRoutes() && ac.getIsDispatch() && SystemData.getBoolean("schedule.flightaware.enabled");
		
		try {
			RouteInfoMessage rmsg = new RouteInfoMessage(usr, msg.getID());
			Connection con = ctx.getConnection();
			RouteLoadHelper helper = new RouteLoadHelper(con, msg.getAirportD(), msg.getAirportA());
			
			// Load dispatch routes
			helper.loadDispatchRoutes();
			
			// If plans is empty and external routes are available, load them
			if (!helper.hasRoutes())
				helper.loadCachedRoutes();
				
			// Go to flightaware if nothing loaded
			if (!helper.hasRoutes() && doExternal)
				helper.loadFlightAwareRoutes(true);
				
			// If we still got nothing, load from existing PIREPs in our database
			if (!helper.hasRoutes()) {
				Collection<AirlineInformation> apps = SystemData.getApps();
				for (Iterator<AirlineInformation> i = apps.iterator(); !helper.hasRoutes() && i.hasNext(); ) {
					AirlineInformation ai = i.next();
					if (ai != null)
						helper.loadPIREPRoutes(ai.getDB());
				}
			}
				
			// Get the departure and arrival weather and calculate the best terminal routes
			helper.loadWeather();
			helper.calculateBestTerminalRoute();
				
			// Populate the routes and add to the message
			helper.populateRoutes();
			for (FlightRoute rt : helper.getRoutes()) {
				if (rt instanceof DispatchRoute)
					((DispatchRoute) rt).setAirline(SystemData.getAirline(ac.getUserData().getAirlineCode()));
					
				rmsg.addPlan((PopulatedRoute) rt);
			}
			
			// Check if the route is valid
			GetSchedule sdao = new GetSchedule(con);
			rmsg.setScheduleInfo(sdao.getFlightNumber(msg.getAirportD(), msg.getAirportA(), ac.getUserData().getDB()));
			
			// Send the response
			if (!ac.getIsDispatch())
				rmsg.setMessage("Loaded " + rmsg.getPlans().size() + " Dispatch routes from database");
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
		return 2750;
	}
}