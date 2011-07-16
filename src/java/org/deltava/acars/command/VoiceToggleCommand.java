// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import org.apache.log4j.Logger;

import org.deltava.beans.mvs.*;
import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.ChannelListMessage;

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
		if ((vtmsg.getVoiceEnabled() == ac.isVoiceEnabled()) && (vtmsg.getVoiceEcho() == ac.isVoiceEcho()))
			log.warn(ac.getUserID() + " requesting status quo - voice " + (ac.isVoiceEnabled() ? "ON" : "OFF"));
		
		// Check access
		if (ac.getUser().getNoVoice()) {
			log.warn(ac.getUserID() + " voice access DISABLED");
			ErrorMessage errMsg = new ErrorMessage(ac.getUser(), "Voice Access Disabled", vtmsg.getID());
			ctx.push(errMsg, env.getConnectionID());
			return;
		}
		
		// Turn on/off voice
		ChannelListMessage clmsg = new ChannelListMessage(ac.getUser(), vtmsg.getID());
		clmsg.setWarnings(ctx.getACARSConnectionPool().getWarnings());
		VoiceChannels vc = VoiceChannels.getInstance();
		if (vtmsg.getVoiceEnabled()) {
			log.info(ac.getUserID() + " enabling Voice - echo = " + vtmsg.getVoiceEcho());
			ac.setVoiceCapable(true);
			ac.setVoiceEcho(vtmsg.getVoiceEcho());
			PopulatedChannel pc = vc.get(ac.getID());
			if (pc == null)
				pc = vc.add(ac, Channel.DEFAULT_NAME);
			
			// Send channel update message
			clmsg.setClearList(false);
			clmsg.add(pc);
		} else {
			log.info(ac.getUserID() + " disabling Voice");
			ac.disableVoice();
			
			// Remove from all channels and send update
			vc.remove(ac.getID());
			clmsg.addAll(vc.getChannels());
		}
		
		// Send an ACK message
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), vtmsg.getID());
		ackMsg.setEntry("echo", String.valueOf(ac.isVoiceEcho()));
		ctx.push(ackMsg, env.getConnectionID());
		ctx.pushVoice(clmsg, ac.getID());
	}
}