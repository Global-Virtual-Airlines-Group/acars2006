// Copyright 2005, 2006, 2014, 2019, 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.*;

import org.deltava.dao.*;

/**
 * An ACARS command to load draft Flight Reports for a Pilot. 
 * @author Luke
 * @version 9.1
 * @since 1.0
 */

public class DraftFlightCommand extends DataCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public final void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		DraftPIREPMessage rspMsg = new DraftPIREPMessage(env.getOwner(), msg.getID());
		String db = ctx.getACARSConnection().getUserData().getDB();
		try {
			GetFlightReports frdao = new GetFlightReports(ctx.getConnection());
			rspMsg.addAll(frdao.getDraftReports(env.getOwner().getID(), null, db));
			ctx.push(rspMsg);
		} catch (DAOException de) {
			log.error("Error loading draft PIREP data for " + msg.getFlag("id") + " - " + de.getMessage(), de);
			ctx.push(new AcknowledgeMessage(env.getOwner(), msg.getID(), "Cannot load draft Flight Report - " + de.getMessage()));
		} finally {
			ctx.release();
		}
	}
}