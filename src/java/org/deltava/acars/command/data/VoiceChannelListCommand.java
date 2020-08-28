// Copyright 2011, 2019, 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;

import org.deltava.acars.message.data.ChannelListMessage;

/**
 * An ACARS command to list voice channels.
 * @author Luke
 * @version 9.1
 * @since 4.0
 */

public class VoiceChannelListCommand extends DataCommand {
	
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