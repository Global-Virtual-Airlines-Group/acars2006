// Copyright 2008, 2010, 2016, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.mp;

import java.util.*;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.mp.RemoveMessage;

/**
 * An ACARS Server command to remove an aircraft from a multi-player session.
 * @author Luke
 * @version 8.6
 * @since 2.2
 */

public class RemoveCommand extends ACARSCommand {
	
	private static final Logger log = Logger.getLogger(RemoveCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message and the ACARS Connection
		RemoveMessage msg = (RemoveMessage) env.getMessage();
		ACARSConnection ac = ctx.getACARSConnection();
		if (ac == null) {
			log.warn("Missing Connection for " + env.getOwnerID());
			return;
		} else if (ac.getIsDispatch()) {
			log.warn("Dispatch Client sending MP Remove Request!");
			return;
		}
		
		// Push message to connections within a set distance
		List<ACARSConnection> cons = ctx.getACARSConnectionPool().getMP(ac.getMPLocation());
		cons.remove(ac);
		cons.forEach(c -> ctx.push(msg, c.getID(), false));
	}
}