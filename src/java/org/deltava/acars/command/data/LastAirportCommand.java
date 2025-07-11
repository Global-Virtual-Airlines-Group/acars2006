// Copyright 2012, 2018, 2019, 2020, 2021, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;

import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.command.*;
import org.deltava.acars.message.AcknowledgeMessage;

import org.deltava.beans.flight.*;

import org.deltava.dao.*;

/**
 * An ACARS Data Command to return the last airport flown into.
 * @author Luke
 * @version 11.1
 * @since 4.1
 */

public class LastAirportCommand extends DataCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		AcknowledgeMessage ackMsg = new AcknowledgeMessage(ctx.getUser(), env.getMessage().getID());
		try {
			GetFlightReports frdao = new GetFlightReports(ctx.getConnection());
			frdao.setQueryMax(15);
			
			// Load all PIREPs and save the latest PIREP as a separate bean in the request
			LogbookSearchCriteria sc = new LogbookSearchCriteria("SUBMITTED DESC", ctx.getDB());
			List<FlightReport> results = frdao.getByPilot(ctx.getUser().getID(), sc);
			Optional<FlightReport> ofr = results.stream().filter(f -> ((f.getStatus() != FlightStatus.DRAFT) && (f.getStatus() != FlightStatus.REJECTED))).findFirst();
			if (ofr.isPresent())
				ackMsg.setEntry("lastAirport", ofr.get().getAirportA().getICAO());
		} catch (DAOException de) {
			log.atError().withThrowable(de).log(de.getMessage());
			ackMsg.setEntry("error", de.getMessage());
		} finally {
			ctx.release();
		}
		
		ctx.push(ackMsg);
	}
}