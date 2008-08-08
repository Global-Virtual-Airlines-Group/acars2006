// Copyright 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.dispatch;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;

import org.deltava.acars.message.dispatch.RangeMessage;

/**
 * An ACARS Command to set the range of ACARS dispatch services.
 * @author Luke
 * @version 2.2
 * @since 2.2
 */

public class ServiceRangeCommand extends DispatchCommand {

	/**
	 * Initializes the Command.
	 */
	public ServiceRangeCommand() {
		super(ServiceRangeCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the inbound message
		RangeMessage msg = (RangeMessage) env.getMessage();
		
		// Set the range and location of the connection
		ACARSConnection ac = ctx.getACARSConnection();
		ac.setDispatchRange(msg.getLocation(), (msg.getRange() < 1) ? Integer.MAX_VALUE : msg.getRange());
	}
}