// Copyright 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.viewer;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.viewer.*;

/**
 * An ACARS Command to handle Flight Viewer acceptance messages. 
 * @author Luke
 * @version 2.8
 * @since 2.8
 */

public class AcceptCommand extends ViewerCommand {

	/**
	 * Initializes the command.
	 */
	public AcceptCommand() {
		super(AcceptCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Cast the message
		AcceptMessage msg = (AcceptMessage) env.getMessage();
		ACARSConnection ac = ctx.getACARSConnection();
		
		// Find the viewer
		ACARSConnection vc = ctx.getACARSConnection(msg.getRecipient());
		if ((vc == null) || !vc.getIsViewer()) {
			log.warn("Viewer " + msg.getRecipient() + " no longer connected or not a viewer");
			
			// Send a cancelation
			CancelMessage cmsg = new CancelMessage(null);
			cmsg.setRecipient(env.getOwnerID());
			ctx.push(cmsg, ac.getID());
			return;
		} else if ((ac.getViewerID() != 0) && (ac.getViewerID() != vc.getID())) {
			log.warn(ac.getUserID() + " already being viewed");
			return;
		}
		
		// Save the viewer
		log.info(ac.getUserID() + " accepted Flight Viewer connection from " + vc.getUserID());
		ac.setViewerID(vc.getID());
		
		// Send the viewer the accept
		ctx.push(msg, vc.getID());
	}
}