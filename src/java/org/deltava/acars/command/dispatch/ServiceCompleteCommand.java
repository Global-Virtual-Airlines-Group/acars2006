// Copyright 2007, 2008, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.dispatch;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;

import org.deltava.acars.message.dispatch.CompleteMessage;
import org.deltava.util.StringUtils;

/**
 * An ACARS Command to handle Dispatch service completion notifications.
 * @author Luke
 * @version 2.7
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
		long dspID = ac.getDispatcherID();
		ACARSConnection dc = ctx.getACARSConnectionPool().get(Long.valueOf(dspID));
		ac.setDispatcherID(0);
		
		// Find the dispatcher to send to
		String dispatcherID = msg.getRecipient();
		if (!StringUtils.isEmpty(dispatcherID)) {
			ACARSConnection dc2 = ctx.getACARSConnection(dispatcherID);
			if ((dc2 != null) && (dc2.getID() != dspID)) {
				dspID = dc2.getID();
				if (dc != null)
					log.warn("Sending Accept to " + dc2.getUserID() + ", expected " + dc.getUserID());
				else
					log.warn("Sending Accept to " + dc2.getUserID());
			} else if (dc2 == null)
				log.warn("Cannot send Accept to " + dispatcherID);
			else
				log.warn("Sending Accept to " + dc2.getUserID());
		} else if (dc == null)
			log.warn("Unknown Dispatch connection - " + dspID);
		else
			log.warn("Sending Accept to " + dc.getUserID());

		// Send to the dispatcher
		ctx.push(msg, dspID);
	}
}