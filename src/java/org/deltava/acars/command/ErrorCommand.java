// Copyright (c) 2005 Delta Virtual Airlines. All Rights Reserved.
package org.deltava.acars.command;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

/**
 * An ACARS Server Command to process error messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class ErrorCommand implements ACARSCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, Envelope env) {
		
		// Get the Message and the ACARS connection
		ErrorMessage msg = (ErrorMessage) env.getMessage();
		ACARSConnection ac = ctx.getACARSConnection();
		
		// Return an acknowledgement with the error message text
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(ac.getUser(), msg.getID());
		ackMsg.setEntry("error", msg.getText());
		
		// Return the message
		ctx.push(ackMsg, ac.getID());
	}
}