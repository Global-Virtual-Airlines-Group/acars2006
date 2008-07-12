// Copyright 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.Iterator;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.ConnectionMessage;

/**
 * An ACARS command to toggle a Pilot's hidden status.
 * @author Luke
 * @version 2.2
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
		
		// Push the response
		for (Iterator<ACARSConnection> i = ctx.getACARSConnections("*").iterator(); i.hasNext(); ) {
			ACARSConnection con = i.next();
			if ((con.getID() != ac.getID()) && !con.getUser().isInRole("HR"))
				ctx.push(dmsg, con.getID());
		}
	}
	
	/**
	 * Returns the maximum execution time of this command before a warning is issued.
	 * @return the maximum execution time in milliseconds
	 */
	public final int getMaxExecTime() {
		return 225;
	}
}