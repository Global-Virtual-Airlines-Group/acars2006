// Copyright 2011, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;

import org.deltava.acars.message.data.ChannelListMessage;

/**
 * An ACARS command to list voice channels.
 * @author Luke
 * @version 8.6
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
		
		// Get the connections
		ChannelListMessage rspmsg = new ChannelListMessage(env.getOwner(), env.getMessage().getID());
		rspmsg.setWarnings(ctx.getACARSConnectionPool().getWarnings());
		rspmsg.addAll(VoiceChannels.getInstance().getChannels());
		ctx.push(rspmsg);
	}
}