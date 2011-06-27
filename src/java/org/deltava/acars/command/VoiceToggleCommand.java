// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.ChannelListMessage;

import org.deltava.beans.mvs.PopulatedChannel;

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
		if (vtmsg.getVoiceEnabled() == ac.isVoiceEnabled())
			log.warn(ac.getUserID() + " requesting status quo - voice " + (ac.isVoiceEnabled() ? "ON" : "OFF"));
		
		// Check access
		if (ac.getUser().getNoVoice()) {
			log.warn(ac.getUserID() + " voice access DISABLED");
			ErrorMessage errMsg = new ErrorMessage(ac.getUser(), "Voice Access Disabled", vtmsg.getID());
			ctx.push(errMsg, env.getConnectionID());
			return;
		}
		
		// Turn on/off voice
		if (vtmsg.getVoiceEnabled()) {
			ac.setVoiceCapable(true);
			log.info(ac.getUserID() + " enabling Voice");
		} else {
			log.info(ac.getUserID() + " disabling Voice");
			ac.disableVoice();
			
			// Get channel that user was in
			PopulatedChannel pc = VoiceChannels.getInstance().get(ac.getID());
			if (pc != null) {
				pc.remove(ac.getID());
				
				// Send channel update message removing the user
				ChannelListMessage clmsg = new ChannelListMessage(ac.getUser(), vtmsg.getID());
				clmsg.setWarnings(ctx.getACARSConnectionPool().getWarnings());
				clmsg.setClearList(false);
				clmsg.add(pc);
				ctx.pushVoice(clmsg, ac.getID());
			}
		}
		
		// Send an ACK message
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), vtmsg.getID());
		ctx.push(ackMsg, env.getConnectionID());
	}
}