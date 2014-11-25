// Copyright 2014 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import org.deltava.beans.UserData;
import org.deltava.beans.schedule.*;

import org.deltava.dao.*;

import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;

import org.deltava.util.StringUtils;
import org.deltava.util.system.SystemData;

/**
 * An ACARS data command to return a flight number.
 * @author Luke
 * @version 5.4
 * @since 5.4
 */

public class FlightNumberCommand extends DataCommand {

	/**
	 * Initializes the Command.
	 */
	public FlightNumberCommand() {
		super(FlightNumberCommand.class);
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

		// Get the Airports
		Airport airportD = SystemData.getAirport(msg.getFlag("airportD"));
		Airport airportA = SystemData.getAirport(msg.getFlag("airportA"));
		if ((airportD == null) || (airportA == null)) {
			rspMsg.setEntry("error", "Unknown Airports");
			ctx.push(rspMsg, env.getConnectionID());
			return;
		}
		
		// Create the route pair
		UserData ud = ctx.getACARSConnection().getUserData();
		String airlineCode = StringUtils.isEmpty(msg.getFlag("airline")) ? ud.getAirlineCode() : msg.getFlag("airline");
		ScheduleRoute rt = new ScheduleRoute(SystemData.getAirline(airlineCode), airportD, airportA);
		try {
			GetSchedule sdao = new GetSchedule(ctx.getConnection());
			ScheduleEntry se = sdao.getFlightNumber(rt, ud.getDB());
			if (se != null) {
				rspMsg.setEntry("airline", se.getAirline().getCode());
				rspMsg.setEntry("flight", String.valueOf(se.getFlightNumber()));
				rspMsg.setEntry("leg", String.valueOf(se.getLeg()));
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