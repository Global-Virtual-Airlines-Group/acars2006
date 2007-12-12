// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.dispatch;

import java.util.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.dispatch.RequestMessage;

/**
 * An ACARS Command to handle Dispatch request messages.
 * @author Luke
 * @version 2.0
 * @since 2.0
 */

public class ServiceRequestCommand extends DispatchCommand {

	/**
	 * Initializes the Command.
	 */
	public ServiceRequestCommand() {
		super(ServiceRequestCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the inbound message
		RequestMessage msg = (RequestMessage) env.getMessage();

		// Send to dispatchers
		Collection<ACARSConnection> cons = ctx.getACARSConnections("*");
		for (Iterator<ACARSConnection> i = cons.iterator(); i.hasNext(); ) {
			ACARSConnection ac = i.next();
			if (ac.getIsDispatch() && !ac.getUserBusy())
				ctx.push(msg, ac.getID(), true);
		}
	}
}