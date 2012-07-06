// Copyright 2008, 2009, 2011, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.Collection;
import java.sql.Connection;

import org.deltava.beans.*;
import org.deltava.beans.flight.*;
import org.deltava.beans.schedule.*;

import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.command.*;
import org.deltava.acars.message.*;

import org.deltava.dao.*;

import org.deltava.util.GeoUtils;
import org.deltava.util.system.SystemData;

/**
 * An ACARS Command to validate that a route exists in the Flight Schedule,
 * and if any dispatch routes currently exist.
 * @author Luke
 * @version 4.2
 * @since 2.3
 */

public class FlightValidationCommand extends DataCommand {

	/**
	 * Initializes the Command.
	 */
	public FlightValidationCommand() {
		super(FlightValidationCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		AcknowledgeMessage rspMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
		UserData ud = ctx.getACARSConnection().getUserData();
		
		// Get the Airports
		Airport airportD = SystemData.getAirport(msg.getFlag("airportD"));
		Airport airportA = SystemData.getAirport(msg.getFlag("airportA"));
		if ((airportD == null) || (airportA == null)) {
			rspMsg.setEntry("error", "Unknown Airports");
			ctx.push(rspMsg, env.getConnectionID());
			return;
		}
		
		// Create the route pair and do ETOPS validation
		ScheduleRoute rt = new ScheduleRoute(SystemData.getAirline(ud.getAirlineCode()), airportD, airportA);
		Collection<GeoLocation> gc = GeoUtils.greatCircle(airportD, airportA, 25);
		ETOPS e = ETOPSHelper.classify(gc).getResult();
		rspMsg.setEntry("etops", String.valueOf(ETOPSHelper.validate(null, e)));
		
		try {
			Connection con = ctx.getConnection();

			// Check the route
			GetSchedule sdao = new GetSchedule(con);
			FlightTime flightTime = sdao.getFlightTime(rt, ud.getDB());
			rspMsg.setEntry("routeOK", String.valueOf(flightTime.getFlightTime() > 0));
			rspMsg.setEntry("hasCurrent", String.valueOf(flightTime.hasCurrent()));
			rspMsg.setEntry("hasHistoric", String.valueOf(flightTime.hasHistoric()));
			
			// Check for dispatch routes
			if (flightTime.getFlightTime() > 0) {
				GetACARSRoute drdao = new GetACARSRoute(con);
				int dRoutes = drdao.getRoutes(airportD, airportA, true).size();
			
				// If we have no dispatch routes, check for cached routes
				if (dRoutes == 0) {
					GetCachedRoutes rcdao = new GetCachedRoutes(con);
					dRoutes = rcdao.getRoutes(airportD, airportA, false).size();
				}
			
				rspMsg.setEntry("dispatchRoutes", String.valueOf(dRoutes));
			}
		} catch (DAOException de) {
			log.error("Error searching Schedule - " + de.getMessage(), de);
			rspMsg.setEntry("error", "Cannot search Flight Schedule - " + de.getMessage());
		} finally {
			ctx.release();
		}

		ctx.push(rspMsg, env.getConnectionID());
	}
}