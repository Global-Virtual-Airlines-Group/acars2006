// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.dispatch;

import java.util.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.dispatch.CancelMessage;

/**
 * An ACARS Command to cancel Dispatch service requests.
 * @author Luke
 * @version 2.0
 * @since 2.0
 */

public class CancelCommand extends DispatchCommand {

	/**
	 * Initializes the Command.
	 */
	public CancelCommand() {
		super(CancelCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the inbound message
		CancelMessage msg = (CancelMessage) env.getMessage();
		
		// Cancel this connection's dispatch status
		ctx.getACARSConnection().setDispatcherID(0);

		// Send to dispatchers
		Collection<ACARSConnection> cons = ctx.getACARSConnections((msg.getRecipient() == null) ? "*" : msg.getRecipient());
		for (Iterator<ACARSConnection> i = cons.iterator(); i.hasNext(); ) {
			ACARSConnection ac = i.next();
			if (ac.getIsDispatch() || (ac.getDispatcherID() == env.getConnectionID()))
				ctx.push(msg, ac.getID(), true);
		}
	}
}