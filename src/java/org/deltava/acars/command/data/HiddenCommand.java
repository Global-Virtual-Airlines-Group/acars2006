// Copyright 2008, 2009, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.ConnectionMessage;

/**
 * An ACARS command to toggle a Pilot's hidden status.
 * @author Luke
 * @version 7.0
 * @since 2.2
 */

public class HiddenCommand extends DataCommand {

	/**
	 * Initializes the command.
	 */
	public HiddenCommand() {
		super(HiddenCommand.class);
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
		if (!ac.getUser().isInRole("HR")) {
			log.warn(ac.getUserID() + " attempting to hide connection!");
			return;	
		}
		
		// Set the pilot's hidden flag
		boolean isNowHidden = Boolean.valueOf(msg.getFlag("isHidden")).booleanValue(); 
		ac.setUserHidden(isNowHidden);
		
		// Send the update message
		int msgType = isNowHidden ? DataMessage.REQ_REMOVEUSER : DataMessage.REQ_ADDUSER;
		ConnectionMessage dmsg = new ConnectionMessage(env.getOwner(), msgType, msg.getID());
		dmsg.add(ac);
		
		// Get a list of authenticated connections
		Collection<ACARSConnection> authCons = ctx.getACARSConnectionPool().getAll();
		for (Iterator<ACARSConnection> i = authCons.iterator(); i.hasNext(); ) {
			ACARSConnection con = i.next();
			if (!con.isAuthenticated())
				i.remove();
		}
		
		// Push the login announcement to everyone not in HR; send a userlist to HR
		for (Iterator<ACARSConnection> i = authCons.iterator(); i.hasNext(); ) {
			ACARSConnection con = i.next();
			if (con.getUser().isInRole("HR")) {
				ConnectionMessage rspMsg = new ConnectionMessage(con.getUser(), DataMessage.REQ_USRLIST, msg.getID());
				rspMsg.addAll(authCons);
				ctx.push(rspMsg, con.getID());
			} else
				ctx.push(dmsg, con.getID());
		}
	}
	
	/**
	 * Returns the maximum execution time of this command before a warning is issued.
	 * @return the maximum execution time in milliseconds
	 */
	@Override
	public final int getMaxExecTime() {
		return 275;
	}
}