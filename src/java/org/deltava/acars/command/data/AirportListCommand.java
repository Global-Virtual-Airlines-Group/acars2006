// Copyright 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;

import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.command.*;
import org.deltava.acars.message.DataRequestMessage;
import org.deltava.acars.message.data.AirportMessage;

import org.deltava.beans.schedule.Airport;
import org.deltava.comparators.AirportComparator;

import org.deltava.util.system.SystemData;

/**
 * An ACARS data command to return available Airport data.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class AirportListCommand extends DataCommand {

	/**
	 * Initializes the Command.
	 */
	public AirportListCommand() {
		super(AirportListCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@SuppressWarnings("unchecked")
	public final void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		AirportMessage rspMsg = new AirportMessage(env.getOwner(), msg.getID());

		// Get all airports
		Map allAirports = (Map) SystemData.getObject("airports");
		Set<Airport> airports = new TreeSet<Airport>(new AirportComparator(AirportComparator.NAME));
		airports.addAll(allAirports.values());
		rspMsg.addAll(airports);
		
		// Push the response
		ctx.push(rspMsg, env.getConnectionID());
	}
}