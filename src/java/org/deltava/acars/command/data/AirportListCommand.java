// Copyright 2006, 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;

import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.command.*;
import org.deltava.acars.message.DataRequestMessage;
import org.deltava.acars.message.data.AirportMessage;

import org.deltava.beans.schedule.Airport;
import org.deltava.comparators.AirportComparator;

import org.deltava.dao.*;

import org.deltava.util.system.SystemData;

/**
 * An ACARS data command to return available Airport data.
 * @author Luke
 * @version 2.3
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
		
		// Load the airports for the user
		Collection<Airport> airports = new TreeSet<Airport>(new AirportComparator(AirportComparator.NAME));
		try {
			GetAirport dao = new GetAirport(ctx.getConnection());
			dao.setAppCode(ctx.getACARSConnection().getUserData().getAirlineCode());
			airports.addAll(dao.getAll().values());
		} catch (DAOException de) {
			log.error("Cannot load airports - " + de.getMessage(), de);
			Map allAirports = (Map) SystemData.getObject("airports");
			airports.addAll(allAirports.values());
		} finally {
			ctx.release();
		}

		// Push the response
		rspMsg.addAll(airports);
		ctx.push(rspMsg, env.getConnectionID());
	}
}