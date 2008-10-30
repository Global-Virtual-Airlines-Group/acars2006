// Copyright 2004, 2005, 2006, 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.ConnectionMessage;

/**
 * An ACARS command to display the connected Pilot list.
 * @author Luke
 * @version 2.2
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
	public final void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		boolean showHidden = ctx.getACARSConnection().getUser().isInRole("HR");

		// Loop through the connection pool
		ConnectionMessage rspMsg = new ConnectionMessage(env.getOwner(), DataMessage.REQ_USRLIST, msg.getID());
		Collection<ACARSConnection> cons = ctx.getACARSConnections("*");
		for (Iterator<ACARSConnection> i = cons.iterator(); i.hasNext(); ) {
			ACARSConnection ac = i.next();
			if (ac.isAuthenticated() && (showHidden || !ac.getUserHidden()))
				rspMsg.add(ac);
		}
		
		// Push the response
		ctx.push(rspMsg, env.getConnectionID());
	}
	
	/**
	 * Returns the maximum execution time of this command before a warning is issued.
	 * @return the maximum execution time in milliseconds
	 */
	public final int getMaxExecTime() {
		return 250;
	}
}