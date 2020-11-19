// Copyright 2005, 2006, 2007, 2016, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import org.deltava.acars.message.*;

import org.deltava.acars.beans.MessageEnvelope;

/**
 * An ACARS Server Command to respond to messages.
 * @author Luke
 * @version 9.1
 * @since 1.0
 */

public class PingCommand extends ACARSCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command cContext
	 * @param env
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		PingMessage msg = (PingMessage) env.getMessage();
		
		// Build the ACK and calculate the time delta
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), env.getMessage().getID());
		if (msg.getClientUTC() != null) {
			long timeDelta = System.currentTimeMillis() - msg.getClientUTC().toEpochMilli();
			ackMsg.setEntry("timeOffset", String.valueOf(timeDelta));
		}
		
		ctx.push(ackMsg);
	}
}