// Copyright 2007, 2009, 2010, 2011, 2016, 2019, 2020, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.dispatch;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;

import org.deltava.acars.message.AcknowledgeMessage;
import org.deltava.acars.message.dispatch.*;

/**
 * An ACARS Command to accept Dispatch service requests. 
 * @author Luke
 * @version 11.1
 * @since 2.0
 */

public class AcceptCommand extends DispatchCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message and the connection
		AcceptMessage msg = (AcceptMessage) env.getMessage();
		ACARSConnection ac = ctx.getACARSConnection(msg.getRecipient());
		if (ac == null) {
			log.warn("Unknown recipient ID - {}", msg.getRecipient());
			return;
		}
		
		// Check dispatch status
		if ((ac.getDispatcherID() == 0) && !ac.getIsDispatch()) {
			ACARSConnection dac = ctx.getACARSConnection(env.getConnectionID());
			ac.setDispatcherID(dac.getID());
			log.info("{} accepted dispatch request from {}", dac.getUserID(), ac.getUserID());
			
			// Send the ACK
			AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getParentID());
			ackMsg.setEntry("dispatcher", env.getOwnerID());
			ackMsg.setEntry("dispatcherID", String.valueOf(env.getOwner().getID()));
			ctx.push(ackMsg, ac.getID(), true);
		} else if (ac.getDispatcherID() != 0)
			log.info("{} already has dispatch service", ac.getUserID());
		
		// Send a cancel message to all other dispatchers
		CancelMessage cMsg = new CancelMessage(ac.getUser());
		ctx.pushDispatch(cMsg, ac.getDispatcherID());
	}
}