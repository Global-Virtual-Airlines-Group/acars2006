// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;
import java.sql.Connection;

import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.TerminalRouteMessage;

import org.deltava.beans.navdata.TerminalRoute;
import org.deltava.dao.GetNavAirway;

import org.deltava.util.system.SystemData;

/**
 * An ACARS command to retrieve all Terminal Route data.
 * @author Luke
 * @version 2.0
 * @since 2.0
 */

public class TerminalRouteCommand extends DataCommand {

	/**
	 * Initializes the Command.
	 */
	public TerminalRouteCommand() {
		super(TerminalRouteCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();

		// Get the terminal routes
		Collection<TerminalRoute> routes = null;
		try {
			Connection con = ctx.getConnection();
			
			// Get the DAO and find the Routes in the DAFIF database
			GetNavAirway dao = new GetNavAirway(con);
			routes = dao.getAll();
		} catch (Exception e) {
			log.error("Error loading terminal routes - " + e.getMessage(), e);
			AcknowledgeMessage errorMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			errorMsg.setEntry("error", "Cannot load Terminal Routes");
			ctx.push(errorMsg, ctx.getACARSConnection().getID());
		} finally {
			ctx.release();
		}
		
		// Only save routes for airports in our database
		Collection<String> codes = SystemData.getAirports().keySet();
		TerminalRouteMessage rspMsg = new TerminalRouteMessage(env.getOwner(), msg.getID());
		for (Iterator<TerminalRoute> i = routes.iterator(); i.hasNext(); ) {
			TerminalRoute tr = i.next();
			if (codes.contains(tr.getICAO()))
				rspMsg.add(tr);
		}
		
		// Push the response
		ctx.push(rspMsg, env.getConnectionID());
	}
}