// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.command;

import org.deltava.acars.beans.Envelope;

import org.deltava.acars.message.*;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class AcknowledgeCommand implements ACARSCommand {

	public void execute(CommandContext ctx, Envelope env) {
		
		Message msg = (Message) env.getMessage(); 
		AcknowledgeMessage aMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
		ctx.push(aMsg, env.getConnectionID());
	}
}