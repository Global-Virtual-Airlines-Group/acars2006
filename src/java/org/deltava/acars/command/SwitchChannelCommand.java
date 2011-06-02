// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.ChannelListMessage;

import org.deltava.beans.mvs.*;

import org.deltava.util.StringUtils;

/**
 * An ACARS command to handle switchng voice channels.
 * @author Luke
 * @version 4.0
 * @since 4.0
 */

public class SwitchChannelCommand extends ACARSCommand {
	
	private static final Logger log = Logger.getLogger(SwitchChannelCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the message and the channel name
		SwitchChannelMessage msg = (SwitchChannelMessage) env.getMessage();
		String chName = msg.getChannel();
		if (StringUtils.isEmpty(chName)) {
			log.warn("Empty channel from " + env.getOwnerID());
			return;
		}
		
		// Try to join the channel
		VoiceChannels vc = VoiceChannels.getInstance();
		ACARSConnection ac = ctx.getACARSConnection();
		try {
			PopulatedChannel pc = vc.add(ac, chName);
			
			// If we didn't add the channel, create a new one
			if (pc == null) {
				Channel c = new Channel(chName);
				c.setOwner(ac.getUser());
				c.setCenter(ac.getLocation());
				c.setSampleRate(SampleRate.SR11K);
				c.addTalkRoles(ac.getUser().getRoles());
				c.addViewRoles(ac.getUser().getRoles());
				c.setIsDefault(vc.size() == 0);
				pc = vc.add(ac, c);
			}
			
			// Return the channel info, to all voice users
			ChannelListMessage cl = new ChannelListMessage(ac.getUser(), msg.getID());
			cl.setClearList(false);
			cl.add(pc);
			ctx.pushVoice(cl, -1);
		} catch (SecurityException se) {
			ctx.push(new ErrorMessage(ac.getUser(), se.getMessage(), msg.getID()), ac.getID());
		}
	}
}