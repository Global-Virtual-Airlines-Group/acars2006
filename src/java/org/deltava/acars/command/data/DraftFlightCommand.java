// Copyright 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.sql.Connection;

import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.*;

import org.deltava.dao.*;

/**
 * An ACARS command to load draft Flight Reports for a Pilot. 
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class DraftFlightCommand extends DataCommand {

	/**
	 * Initializes the command.
	 */
	public DraftFlightCommand() {
		super(DraftFlightCommand.class);
	}
	
	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public final void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();

		DraftPIREPMessage rspMsg = new DraftPIREPMessage(env.getOwner(), msg.getID());
		try {
			Connection con = ctx.getConnection();

			// Get the DAO and the flight report
			String db = ctx.getACARSConnection().getUserData().getDB();
			GetFlightReports frdao = new GetFlightReports(con);
			rspMsg.addAll(frdao.getDraftReports(env.getOwner().getID(), null, null, db));
		} catch (DAOException de) {
			log.error("Error loading draft PIREP data for " + msg.getFlag("id") + " - " + de.getMessage(), de);
			AcknowledgeMessage errMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			errMsg.setEntry("error", "Cannot load draft Flight Report");
			ctx.push(errMsg, ctx.getACARSConnection().getID());
		} finally {
			ctx.release();
		}

		// Push the response
		ctx.push(rspMsg, env.getConnectionID());
	}
}