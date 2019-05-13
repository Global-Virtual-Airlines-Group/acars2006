// Copyright 2005, 2006, 2016, 2018, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.GenericMessage;

import org.deltava.util.system.SystemData;

/**
 * An ACARS Server data command to return the Private Voice URL.
 * @author Luke
 * @version 8.6
 * @since 1.0
 */

public class PrivateVoiceCommand extends DataCommand {

	/**
	 * Initializes the Command.
	 */
	public PrivateVoiceCommand() {
		super(PrivateVoiceCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public final void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		
		// Generate the response
		GenericMessage rspMsg = new GenericMessage(env.getOwner(), DataRequest.PVTVOX, msg.getID());
		rspMsg.setLabel("url");
		rspMsg.add(SystemData.get("airline.voice.url"));
		ctx.push(rspMsg);
	}
}