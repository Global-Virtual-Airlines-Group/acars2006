// Copyright 2005, 2006, 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.message.*;

/**
 * An ACARS Server Command to respond to messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class AcknowledgeCommand extends ACARSCommand {

	private boolean _result;

	/**
	 * Initializes the Command.
	 * @param result whether to send an ACK or not
	 */
	public AcknowledgeCommand(boolean result) {
		super();
		_result = result;
	}

	/**
	 * Executes the command.
	 * @param ctx the Command cContext
	 * @param env
	 */
	public void execute(CommandContext ctx, MessageEnvelope env) {
		if (!_result)
			return;

		// Send an ACK
		AcknowledgeMessage aMsg = new AcknowledgeMessage(env.getOwner(), env.getMessage().getID());
		ctx.push(aMsg, env.getConnectionID());
	}
}