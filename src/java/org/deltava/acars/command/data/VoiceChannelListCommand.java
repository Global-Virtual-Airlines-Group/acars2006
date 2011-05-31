// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;

import org.deltava.acars.message.DataRequestMessage;
import org.deltava.acars.message.data.ChannelListMessage;

import org.deltava.beans.mvs.*;

import org.deltava.util.RoleUtils;

/**
 * An ACARS command to list voice channels.
 * @author Luke
 * @version 4.0
 * @since 4.0
 */

public class VoiceChannelListCommand extends DataCommand {
	
	/**
	 * Initializes the command.
	 */
	public VoiceChannelListCommand() {
		super(VoiceChannelListCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		
		// Get the connections
		ChannelListMessage rspmsg = new ChannelListMessage(env.getOwner(), msg.getID());
		for (PopulatedChannel pc : VoiceChannels.getChannels()) {
			Channel c = pc.getChannel();
			
			// Check if we can view the channel
			if (RoleUtils.hasAccess(ctx.getUser().getRoles(), c.getViewRoles()))
				rspmsg.add(pc);
		}
		
		ctx.push(rspmsg, env.getConnectionID());
	}
}