// Copyright 2012, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;

import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.command.*;
import org.deltava.acars.message.AcknowledgeMessage;

import org.deltava.beans.flight.*;
import org.deltava.beans.schedule.ScheduleSearchCriteria;

import org.deltava.dao.*;

/**
 * An ACARS Data Command to return the last airport flown into.
 * @author Luke
 * @version 8.1
 * @since 4.1
 */

public class LastAirportCommand extends DataCommand {

	/**
	 * Initializes the command.
	 */
	public LastAirportCommand() {
		super(LastAirportCommand.class);
	}
	
	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Create the acknowledgement
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(ctx.getUser(), env.getMessage().getID());

		ScheduleSearchCriteria ssc = new ScheduleSearchCriteria("SUBMITTED DESC");
		ssc.setDBName(ctx.getACARSConnection().getUserData().getDB());
		try {
			GetFlightReports frdao = new GetFlightReports(ctx.getConnection());
			frdao.setQueryMax(15);
			
			// Load all PIREPs and save the latest PIREP as a separate bean in the request
			List<FlightReport> results = frdao.getByPilot(ctx.getUser().getID(), ssc);
			for (FlightReport fr : results) {
				if ((fr.getStatus() != FlightStatus.DRAFT) && (fr.getStatus() != FlightStatus.REJECTED)) {
					ackMsg.setEntry("lastAirport", fr.getAirportA().getICAO());
					break;
				}
			}
		} catch (DAOException de) {
			log.error(de.getMessage(), de);
			ackMsg.setEntry("error", de.getMessage());
		} finally {
			ctx.release();
		}
		
		ctx.push(ackMsg, ctx.getACARSConnection().getID());
	}
}