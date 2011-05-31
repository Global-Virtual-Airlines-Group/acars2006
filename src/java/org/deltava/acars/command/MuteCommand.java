// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.MuteMessage;

/**
 * An ACARS command to mute/unmute a user.
 * @author Luke
 * @version 4.0
 * @since 4.0
 */

public class MuteCommand extends ACARSCommand {
	
	private static final Logger log = Logger.getLogger(MuteCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		MuteMessage msg = (MuteMessage) env.getMessage();

		// Get the user
		ACARSConnection ac = ctx.getACARSConnection(msg.getRecipient());
		if (ac == null) {
			log.info("Unknown user ID - " + msg.getRecipient());
			return;
		}
		
		// Set and log new mute status
		boolean newMute = !ac.getMuted();
		ac.setMuted(newMute);
		log.info(env.getOwner().getName() + " set " + ac.getUserID() + " mute status = " + newMute);
	}
}