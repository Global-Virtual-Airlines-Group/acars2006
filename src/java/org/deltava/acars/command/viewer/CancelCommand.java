// Copyright 2009, 2010 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.viewer;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;

import org.deltava.acars.message.viewer.CancelMessage;

/**
 * An ACARS Server command to send Flight Viewer cancellations.
 * @author Luke
 * @version 2.8
 * @since 2.8
 */

public class CancelCommand extends ViewerCommand {
	
	/**
	 * Initializes the command.
	 */
	public CancelCommand() {
		super(CancelCommand.class);
	}
	
	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Cast the message
		CancelMessage cmsg = (CancelMessage) env.getMessage();
		ACARSConnection con = ctx.getACARSConnection();
		
		// If we are a viewer, find the connection I am viewing and turn it off. Otherwise send it to my viewer.
		log.info("Received Flight Viewer cancelation from " + con.getUserID());
		ACARSConnection ac = ctx.getACARSConnection(cmsg.getRecipient());
		if ((ac != null) && con.getIsViewer() && (ac.getViewerID() == con.getID()))
			ac.setViewerID(0);
		else if (!con.getIsViewer())
			con.setViewerID(0);
		
		// Send the message 
		if (ac != null)
			ctx.push(cmsg, ac.getID());
	}
}