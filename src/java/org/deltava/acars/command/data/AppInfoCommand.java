// Copyright 2011, 2019, 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.message.data.AppInfoMessage;

import org.deltava.acars.command.*;

import org.deltava.util.system.SystemData;

/**
 * An ACARS server command to display cross-application information. 
 * @author Luke
 * @version 9.1
 * @since 3.6
 */

public class AppInfoCommand extends DataCommand {
	
	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Build the response
		AppInfoMessage rspMsg = new AppInfoMessage(env.getOwner(), env.getMessage().getID());
		SystemData.getApps().forEach(rspMsg::add);
		ctx.push(rspMsg);
	}
}