// Copyright 2005, 2006, 2016, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

/**
 * An ACARS Server Command to process error messages.
 * @author Luke
 * @version 8.6
 * @since 1.0
 */

public class ErrorCommand extends ACARSCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the Message and the ACARS connection
		ErrorMessage msg = (ErrorMessage) env.getMessage();
		ACARSConnection ac = ctx.getACARSConnection();
		
		// Return an acknowledgement with the error message text
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(ac.getUser(), msg.getID());
		ackMsg.setEntry("error", msg.getText());
		ctx.push(ackMsg);
	}
}