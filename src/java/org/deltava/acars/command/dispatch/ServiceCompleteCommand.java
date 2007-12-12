// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.dispatch;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;

import org.deltava.acars.message.dispatch.CompleteMessage;

/**
 * An ACARS Command to handle Dispatch service completion notifications.
 * @author Luke
 * @version 2.0
 * @since 2.0
 */

public class ServiceCompleteCommand extends DispatchCommand {
	
	/**
	 * Initializes the Command.
	 */
	public ServiceCompleteCommand() {
		super(ServiceCompleteCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the inbound message
		CompleteMessage msg = (CompleteMessage) env.getMessage();

		// Cancel this connection's dispatch status
		ACARSConnection ac = ctx.getACARSConnection();

		// Send to the dispatcher
		ctx.push(msg, ac.getDispatcherID());
	}
}