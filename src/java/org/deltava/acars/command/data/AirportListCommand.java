// Copyright 2006, 2008, 2010, 2011, 2016, 2018, 2019, 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;

import org.deltava.acars.beans.*;
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
 * @version 9.1
 * @since 1.0
 */

public class AirportListCommand extends DataCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public final void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		AirportMessage rspMsg = new AirportMessage(env.getOwner(), msg.getID());
		
		// Get airline code - for dispatcher it should be ALL
		ACARSConnection ac = ctx.getACARSConnection();
		String appCode = ac.getIsDispatch() ? "ALL" : ac.getUserData().getAirlineCode();
		
		// Load the airports for the user
		Collection<Airport> airports = new TreeSet<Airport>(new AirportComparator(AirportComparator.NAME));
		try {
			GetAirport dao = new GetAirport(ctx.getConnection());
			dao.setAppCode(appCode);
			airports.addAll(dao.getAll().values());
			airports.addAll(dao.getEventAirports());
		} catch (DAOException de) {
			log.error("Cannot load airports - " + de.getMessage(), de);
			@SuppressWarnings("unchecked")
			Map<?, Airport> allAirports = (Map<?, Airport>) SystemData.getObject("airports");
			airports.addAll(allAirports.values());
		} finally {
			ctx.release();
		}

		rspMsg.addAll(airports);
		ctx.push(rspMsg);
	}
}