// Copyright 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.message.*;

import org.deltava.util.system.SystemData;

/**
 * An ACARS Server Command to respond to ACK messages
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class AcknowledgeCommand extends ACARSCommand {

	private String ackType;

	/**
	 * Intiailizes the Command.
	 * @param ackType the original message type
	 */
	public AcknowledgeCommand(String ackType) {
		super();
		ackType = ackType.toLowerCase();
	}

	/**
	 * Executes the command.
	 * @param ctx the Command cContext
	 * @param env
	 */
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Check if we should acknowledge this message
		if (SystemData.getBoolean("acars.ack." + ackType)) {
			Message msg = env.getMessage();
			AcknowledgeMessage aMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			ctx.push(aMsg, env.getConnectionID());
		}
	}
}