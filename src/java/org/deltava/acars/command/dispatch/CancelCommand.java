// Copyright 2007, 2008, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.dispatch;

import java.util.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.dispatch.CancelMessage;

/**
 * An ACARS Command to cancel Dispatch service requests.
 * @author Luke
 * @version 2.7
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
		ACARSConnection con = ctx.getACARSConnection();
		if (!con.getIsDispatch())
			con.setDispatcherID(0);
		
		// Get the dispatchers
		Collection<ACARSConnection> cons = new ArrayList<ACARSConnection>();
		if (msg.getRecipient() != null) {
			ACARSConnection ac = ctx.getACARSConnection(msg.getRecipient());
			if (ac != null)
				cons.add(ac);
		} else
			cons.addAll(ctx.getACARSConnectionPool().getAll());

		// Send to dispatchers
		for (Iterator<ACARSConnection> i = cons.iterator(); i.hasNext(); ) {
			ACARSConnection ac = i.next();
			if (ac.getIsDispatch() || (ac.getDispatcherID() == env.getConnectionID()))
				ctx.push(msg, ac.getID(), true);
		}
	}
}