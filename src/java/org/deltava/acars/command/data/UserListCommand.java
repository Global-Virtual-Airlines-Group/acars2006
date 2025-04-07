// Copyright 2004, 2005, 2006, 2008, 2009, 2016, 2018, 2019, 2020, 2025 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.ConnectionMessage;
import org.deltava.beans.Pilot;

/**
 * An ACARS command to display the connected Pilot list.
 * @author Luke
 * @version 11.6
 * @since 1.0
 */

public class UserListCommand extends DataCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public final void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		Pilot p = ctx.getACARSConnection().getUser();
		if (p == null) return;
		
		// Loop through the connection pool
		ConnectionMessage rspMsg = new ConnectionMessage(env.getOwner(), DataRequest.USERLIST, msg.getID());
		Collection<ACARSConnection> cons = ctx.getACARSConnectionPool().getAll();
		cons.stream().filter(ac -> filter(ac, p.isInRole("HR"))).forEach(rspMsg::add);
		ctx.push(rspMsg);
	}
	
	private static boolean filter(ACARSConnection ac, boolean showHidden) {
		return ac.isAuthenticated() && (showHidden || !ac.getUserHidden());
	}
	
	@Override
	public final int getMaxExecTime() {
		return 125;
	}
}