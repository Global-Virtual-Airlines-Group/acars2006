// Copyright 2015, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

/**
 * An ACARS Server Command to enable/disable data compression. 
 * @author Luke
 * @version 7.0
 * @since 6.4
 */

public class CompressionCommand extends ACARSCommand {
	
	private static final Logger log = Logger.getLogger(CompressionCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message and the connection
		ACARSConnection ac = ctx.getACARSConnection();
		final CompressionMessage msg = (CompressionMessage) env.getMessage(); 
		String cType = msg.getCompression().name().toLowerCase(); 
		if (ac.getCompression() == msg.getCompression()) {
			log.warn(ac.getUserID() + " requesting " + cType + " compression, already set");
			return;
		}
		
		// Set compression for connection
		log.info("Setting data compression for " + ac.getUserID() + " to " + cType);
		ac.setCompression(msg.getCompression());

		// Send an ACK
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
		ackMsg.setEntry("type", cType);
		ctx.push(ackMsg, ac.getID());
	}
}