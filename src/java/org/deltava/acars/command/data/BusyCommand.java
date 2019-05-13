// Copyright 2005, 2006, 2016, 2018, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;

import org.deltava.acars.message.*;
import org.deltava.acars.message.data.ConnectionMessage;

/**
 * An ACARS command to toggle a Pilot's busy status.
 * @author Luke
 * @version 8.6
 * @since 1.0
 */

public class BusyCommand extends DataCommand {

	/**
	 * Initializes the command.
	 */
	public BusyCommand() {
		super(BusyCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public final void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message and connection
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		ACARSConnection ac = ctx.getACARSConnection();
		
		// Set the pilot's busy flag
		ac.setUserBusy(Boolean.valueOf(msg.getFlag("isBusy")).booleanValue());
		
		// Push the update to everyone, only us if we're hidden
		ConnectionMessage rspMsg = new ConnectionMessage(env.getOwner(), DataRequest.BUSY, msg.getID());
		rspMsg.add(ac);
		if (ac.getUserHidden())
			ctx.push(rspMsg);
		else
			ctx.pushAll(rspMsg, 0);
	}
	
	/**
	 * Returns the maximum execution time of this command before a warning is issued.
	 * @return the maximum execution time in milliseconds
	 */
	@Override
	public final int getMaxExecTime() {
		return 200;
	}
}