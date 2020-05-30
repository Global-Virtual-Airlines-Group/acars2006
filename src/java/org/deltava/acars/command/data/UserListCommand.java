// Copyright 2004, 2005, 2006, 2008, 2009, 2016, 2018, 2019, 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.ConnectionMessage;

/**
 * An ACARS command to display the connected Pilot list.
 * @author Luke
 * @version 9.0
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
		boolean showAll = ctx.getACARSConnection().getUser().getRoles().contains("Developer");
		String myCode = ctx.getACARSConnection().getUserData().getAirlineCode();

		// Loop through the connection pool
		ConnectionMessage rspMsg = new ConnectionMessage(env.getOwner(), DataRequest.USERLIST, msg.getID());
		Collection<ACARSConnection> cons = ctx.getACARSConnectionPool().getAll();
		cons.stream().filter(ac -> filter(ac, myCode, showHidden, showAll)).forEach(rspMsg::add);
		ctx.push(rspMsg);
	}
	
	private static boolean filter(ACARSConnection ac, String airlineCode, boolean showHidden, boolean showAll) {
		if (!ac.isAuthenticated()) return false;
		if (!airlineCode.equals(ac.getUserData().getAirlineCode()) && !showAll) return false;
		return showHidden || !ac.getUserHidden();
	}
	
	/**
	 * Returns the maximum execution time of this command before a warning is issued.
	 * @return the maximum execution time in milliseconds
	 */
	@Override
	public final int getMaxExecTime() {
		return 150;
	}
}