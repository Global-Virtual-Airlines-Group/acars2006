// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

/**
 * An ACARS command to toggle voice support. 
 * @author Luke
 * @version 4.0
 * @since 4.0
 */

public class VoiceToggleCommand extends ACARSCommand {
	
	private static final Logger log = Logger.getLogger(VoiceToggleCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the message
		VoiceToggleMessage vtmsg = (VoiceToggleMessage) env.getMessage();
		
		// Check voice status
		ACARSConnection ac = ctx.getACARSConnection();
		if (vtmsg.getVoiceEnabled() == ac.isVoiceEnabled()) {
			log.warn(ac.getUserID() + " requesting status quo - voice " + (ac.isVoiceEnabled() ? "ON" : "OFF"));
			return;
		}
		
		// Check access
		if (ac.getUser().getNoVoice()) {
			log.warn(ac.getUserID() + " voice access DISABLED");
			// TODO: Send an error message here
			return;
		}
		
		// Turn on/off voice
		if (vtmsg.getVoiceEnabled()) {
			log.info(ac.getUserID() + " enabling Voice");
			ac.connectVoice();
		} else {
			log.info(ac.getUserID() + " disabling Voice");
			ac.disconnectVoice();
		}
		
		// Send an ACK message
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), vtmsg.getID());
		ctx.push(ackMsg, env.getConnectionID());
	}
}