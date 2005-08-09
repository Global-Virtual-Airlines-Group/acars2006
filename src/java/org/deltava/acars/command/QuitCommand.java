// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.command;

import org.deltava.acars.beans.Envelope;

import org.deltava.acars.message.DataMessage;
import org.deltava.acars.message.DataResponseMessage;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class QuitCommand implements ACARSCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, Envelope env) {

		// Create a deletepilots message
		DataResponseMessage msg = new DataResponseMessage(env.getOwner(), DataMessage.REQ_REMOVEUSER);
		msg.addResponse(env.getOwner());
		
		// Send to everyone except ourself
		ctx.pushAll(msg, env.getConnectionID());
	}
}