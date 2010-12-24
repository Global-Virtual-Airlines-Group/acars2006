// Copyright 2009, 2010 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.viewer;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.viewer.*;

/**
 * An ACARS Server command to send Flight Viewer requests.
 * @author Luke
 * @version 3.4
 * @since 2.8
 */

public class RequestCommand extends ViewerCommand {
	
	/**
	 * Initializes the command.
	 */
	public RequestCommand() {
		super(RequestCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Cast the message
		RequestMessage reqmsg = (RequestMessage) env.getMessage();
		ACARSConnection ac = ctx.getACARSConnection();
		if (!ac.getUser().isInRole("Instructor")) {
			log.warn(ac.getUserID() + " requesting Flight Viewer!");
			return;
		}
		
		// Get the recipient
		ACARSConnection ac2 = ctx.getACARSConnection(reqmsg.getRecipient());
		if (ac2 == null) {
			log.warn("User " + reqmsg.getRecipient() + " not connected");
			return;
		}
		
		// If the connection already thinks it's connected to me, send an Acceptance
		if (ac2.getViewerID() == ac.getID()) {
			log.info(reqmsg.getRecipient() + " already being viewed by " + reqmsg.getSenderID());
			AcceptMessage amsg = new AcceptMessage(ac2.getUser());
			amsg.setRecipient(ac.getUserID());
			ctx.push(amsg, ac.getID());
			return;
		}
		
		// Send the message along
		ctx.push(reqmsg, ac2.getID());
	}
}