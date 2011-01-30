// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.message.data.AppInfoMessage;

import org.deltava.acars.command.*;

import org.deltava.beans.system.AirlineInformation;

import org.deltava.util.system.SystemData;

/**
 * An ACARS server command to display cross-application information. 
 * @author Luke
 * @version 3.6
 * @since 3.6
 */

public class AppInfoCommand extends DataCommand {
	
	/**
	 * Initializes the Command.
	 */
	public AppInfoCommand() {
		super(AppInfoCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Build the response
		AppInfoMessage rspMsg = new AppInfoMessage(env.getOwner(), env.getMessage().getID());
		for (AirlineInformation ai : SystemData.getApps()) {
			if (ai != null)
				rspMsg.add(ai);
		}
		
		ctx.push(rspMsg, env.getConnectionID());
	}
}