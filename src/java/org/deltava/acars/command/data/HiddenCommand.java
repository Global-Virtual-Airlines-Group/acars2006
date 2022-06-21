// Copyright 2008, 2009, 2016, 2018, 2019, 2020, 2022 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.ConnectionMessage;

/**
 * An ACARS command to toggle a Pilot's hidden status.
 * @author Luke
 * @version 10.2
 * @since 2.2
 */

public class HiddenCommand extends DataCommand {

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
		if (!ac.getUser().isInRole("HR")) {
			log.warn(ac.getUserID() + " attempting to hide connection!");
			return;	
		}
		
		// Set the pilot's hidden flag
		boolean isNowHidden = Boolean.parseBoolean(msg.getFlag("isHidden")); 
		ac.setUserHidden(isNowHidden);
		
		// Send the update message
		DataRequest msgType = isNowHidden ? DataRequest.REMOVEUSER : DataRequest.ADDUSER;
		ConnectionMessage dmsg = new ConnectionMessage(env.getOwner(), msgType, msg.getID());
		dmsg.add(ac);
		
		// Push the login announcement to everyone not in HR; send a userlist to HR
		Collection<ACARSConnection> authCons = ctx.getACARSConnectionPool().getAll(ACARSConnection::isAuthenticated);
		for (ACARSConnection con : authCons) {
			if (con.getUser().isInRole("HR")) {
				ConnectionMessage rspMsg = new ConnectionMessage(con.getUser(), DataRequest.USERLIST, msg.getID());
				rspMsg.addAll(authCons);
				ctx.push(rspMsg, con.getID(), false);
			} else
				ctx.push(dmsg, con.getID(), false);
		}
	}
	
	@Override
	public final int getMaxExecTime() {
		return 225;
	}
}