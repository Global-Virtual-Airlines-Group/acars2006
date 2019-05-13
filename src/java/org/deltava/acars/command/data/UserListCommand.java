// Copyright 2004, 2005, 2006, 2008, 2009, 2016, 2018, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.ConnectionMessage;

/**
 * An ACARS command to display the connected Pilot list.
 * @author Luke
 * @version 8.6
 * @since 1.0
 */

public class UserListCommand extends DataCommand {

	/**
	 * Initializes the Command.
	 */
	public UserListCommand() {
		super(UserListCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public final void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		boolean showHidden = ctx.getACARSConnection().getUser().isInRole("HR");

		// Loop through the connection pool
		ConnectionMessage rspMsg = new ConnectionMessage(env.getOwner(), DataRequest.USERLIST, msg.getID());
		Collection<ACARSConnection> cons = ctx.getACARSConnectionPool().getAll();
		cons.stream().filter(ac -> ac.isAuthenticated() && (showHidden || !ac.getUserHidden())).forEach(rspMsg::add);
		ctx.push(rspMsg);
	}
	
	/**
	 * Returns the maximum execution time of this command before a warning is issued.
	 * @return the maximum execution time in milliseconds
	 */
	@Override
	public final int getMaxExecTime() {
		return 225;
	}
}