// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.dispatch;

import java.util.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;

import org.deltava.acars.message.AcknowledgeMessage;
import org.deltava.acars.message.dispatch.*;

/**
 * An ACARS Command to accept Dispatch service requests. 
 * @author Luke
 * @version 2.0
 * @since 2.0
 */

public class AcceptCommand extends DispatchCommand {

	/**
	 * Initializes the Command.
	 */
	public AcceptCommand() {
		super(AcceptCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		AcceptMessage msg = (AcceptMessage) env.getMessage();
		
		// Get the connection
		Collection<ACARSConnection> cons = ctx.getACARSConnections(msg.getRecipient());
		if (cons.isEmpty()) {
			log.warn("Unknown recipient ID - " + msg.getRecipient());
			return;
		}
		
		// Get the recipients and check dispatch status
		for (Iterator<ACARSConnection> i = cons.iterator(); i.hasNext(); ) {
			ACARSConnection ac = i.next();
			if (ac.getHasDispatch()) {
				log.info(ac.getUserID() + " already has dispatch service");
				return;
			} else if (!ac.getIsDispatch()) {
				ac.setDispatcherID(env.getConnectionID());
				AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getParentID());
				ackMsg.setEntry("dispatcher", env.getOwnerID());
				ctx.push(ackMsg, ac.getID());
			}
		}
		
		// Send a cancel message to all other dispatchers
		CancelMessage cMsg = new CancelMessage(env.getOwner());
		ctx.pushDispatch(cMsg, env.getConnectionID());
	}
}