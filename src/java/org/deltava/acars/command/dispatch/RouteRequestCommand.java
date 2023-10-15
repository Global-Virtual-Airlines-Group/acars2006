// Copyright 2007, 2008, 2009, 2010, 2012, 2016, 2018, 2019, 2020, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.dispatch;

import java.util.*;
import java.sql.Connection;

import org.deltava.beans.*;
import org.deltava.beans.acars.*;
import org.deltava.beans.schedule.*;
import org.deltava.beans.system.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.AcknowledgeMessage;
import org.deltava.acars.message.dispatch.*;

import org.deltava.acars.command.*;

import org.deltava.dao.*;

import org.deltava.util.system.SystemData;

/**
 * An ACARS Dispatch Command to load flight routes.
 * @author Luke
 * @version 11.0
 * @since 2.0
 */

public class RouteRequestCommand extends DispatchCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the inbound message and the owner
		Pilot usr = env.getOwner();
		ACARSConnection ac = ctx.getACARSConnection();
		RouteRequestMessage msg = (RouteRequestMessage) env.getMessage();
		boolean doExternal = msg.getExternalRoutes() && ac.getIsDispatch() && SystemData.getBoolean("schedule.flightaware.enabled");
		
		try {
			RouteInfoMessage rmsg = new RouteInfoMessage(usr, msg.getID());
			Connection con = ctx.getConnection();
			RouteLoadHelper helper = new RouteLoadHelper(con, msg);
			
			// Load dispatch routes
			helper.loadDispatchRoutes();
			
			// If plans is empty and external routes are available, load them
			if (!helper.hasRoutes())
				helper.loadCachedRoutes();
				
			// Go to flightaware if nothing loaded
			if (!helper.hasRoutes() && doExternal) {
				APILogger.add(new APIRequest(API.FlightAware.createName("ROUTES"), ac.getUserData().getDB(), !ac.isAuthenticated(), false));
				helper.loadFlightAwareRoutes(true);
			}
				
			// If we still got nothing, load from existing PIREPs in our database
			Collection<AirlineInformation> apps = SystemData.getApps();
			for (Iterator<AirlineInformation> i = apps.iterator(); !helper.hasRoutes() && i.hasNext(); ) {
				AirlineInformation ai = i.next();
				if (ai != null)
					helper.loadPIREPRoutes(ai.getDB());
			}
				
			// Get the departure and arrival weather and calculate the best terminal routes
			helper.loadWeather();
			helper.calculateBestTerminalRoute();
				
			// Populate the routes and add to the message
			helper.populateRoutes();
			for (FlightRoute rt : helper.getRoutes()) {
				if (rt instanceof DispatchRoute dr)
					dr.setAirline(SystemData.getAirline(ac.getUserData().getAirlineCode()));
					
				rmsg.addPlan((PopulatedRoute) rt);
			}
			
			// Check if the route is valid
			UserData ud = ac.getUserData();
			GetRawSchedule rsdao = new GetRawSchedule(con);
			GetSchedule sdao = new GetSchedule(con);
			sdao.setSources(rsdao.getSources(true, ud.getDB()));
			rmsg.setScheduleInfo(sdao.getFlightNumber(msg, ud.getDB()));
			
			// Send the response
			if (!ac.getIsDispatch())
				rmsg.setMessage("Loaded " + rmsg.getPlans().size() + " Dispatch routes from database");
			ctx.push(rmsg);
		} catch (DAOException de) {
			log.atError().withThrowable(de).log("Cannot load route data - {}", de.getMessage());
			ctx.push(new AcknowledgeMessage(env.getOwner(), msg.getID(), "Cannot load route data - " + de.getMessage()));
		} finally {
			ctx.release();
		}
	}
	
	@Override
	public final int getMaxExecTime() {
		return 2750;
	}
}