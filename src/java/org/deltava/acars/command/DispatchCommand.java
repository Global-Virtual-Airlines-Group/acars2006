// Copyright 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;

import org.apache.log4j.Logger;

import org.deltava.beans.Pilot;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

/**
 * An ACARS server command to process Dispatch Messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class DispatchCommand extends ACARSCommand {
	
	private static final Logger log = Logger.getLogger(DispatchCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, Envelope env) {

		// Get the inbound message and the owner
		Pilot usr = env.getOwner();
		DispatchMessage msg = (DispatchMessage) env.getMessage();
		if ((usr == null) || (!usr.isInRole("Dispatch"))) {
			log.warn("Unauthorized dispatch message from " + env.getConnectionID());
			return;
		}
		
		// Create the ACK message
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(usr, msg.getID());

		// Get the recipient
		Collection<ACARSConnection> dstC = ctx.getACARSConnections(msg.getRecipient());
		if (dstC.isEmpty()) {
			ackMsg.setEntry("error", "Unknown recipient - " + msg.getRecipient());
			log.warn("Cannot send dispatch message to " + msg.getRecipient());
		}
		
		// Send out the dispatch data
		ackMsg.setEntry("msgs", String.valueOf(dstC.size()));
		for (Iterator<ACARSConnection> i = dstC.iterator(); i.hasNext(); ) {
			ACARSConnection ac = i.next();
			log.info("Dispatch info from " + usr.getPilotCode() + " to " + ac.getUserID());
			ctx.push(msg, ac.getID());
		}
		
		// Send out the ack
		ctx.push(ackMsg, ctx.getACARSConnection().getID());
	}
}