// Copyright 2008, 2009, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.sql.Connection;

import org.deltava.beans.schedule.Airport;
import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.command.*;
import org.deltava.acars.message.*;

import org.deltava.dao.*;

import org.deltava.util.system.SystemData;

/**
 * An ACARS Command to validate that a route exists in the Flight Schedule,
 * and if any dispatch routes currently exist.
 * @author Luke
 * @version 4.0
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
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		AcknowledgeMessage rspMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
		
		// Get the Airports
		Airport airportD = SystemData.getAirport(msg.getFlag("airportD"));
		Airport airportA = SystemData.getAirport(msg.getFlag("airportA"));
		if ((airportD == null) || (airportA == null)) {
			rspMsg.setEntry("error", "Unknown Airports");
			ctx.push(rspMsg, env.getConnectionID());
			return;
		}
		
		try {
			Connection con = ctx.getConnection();

			// Check the route
			GetSchedule sdao = new GetSchedule(con);
			int flightTime = sdao.getFlightTime(airportD, airportA, ctx.getACARSConnection().getUserData().getDB());
			rspMsg.setEntry("routeOK", String.valueOf(flightTime > 0));
			
			// Check for dispatch routes
			GetACARSRoute drdao = new GetACARSRoute(con);
			int dRoutes = drdao.getRoutes(airportD, airportA, true).size();
			
			// If we have no dispatch routes, check for cached routes
			if (dRoutes == 0) {
				GetCachedRoutes rcdao = new GetCachedRoutes(con);
				dRoutes = rcdao.getRoutes(airportD, airportA, false).size();
			}
			
			// Save dispatch routes
			rspMsg.setEntry("dispatchRoutes", String.valueOf(dRoutes));
		} catch (DAOException de) {
			log.error("Error searching Schedule - " + de.getMessage(), de);
			rspMsg.setEntry("error", "Cannot search Flight Schedule - " + de.getMessage());
		} finally {
			ctx.release();
		}

		// Push the response
		ctx.push(rspMsg, env.getConnectionID());
	}
}