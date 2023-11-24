// Copyright 2010, 2019, 2020, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.dispatch;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.AcknowledgeMessage;
import org.deltava.acars.message.dispatch.ScopeInfoMessage;

import org.deltava.acars.command.*;
import org.deltava.util.system.SystemData;

/**
 * An ACARS Dispatch Command for radar scope information messages. 
 * @author Luke
 * @version 11.1
 * @since 3.0
 */

public class ScopeInfoCommand extends DispatchCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the message and the connection
		ScopeInfoMessage msg = (ScopeInfoMessage) env.getMessage();
		ACARSConnection ac = ctx.getACARSConnection();
		
		// Check the max range - if we're not an Admin/HR limit it
		int maxRange = SystemData.getInt("mp.max_scope_range", 1500);
		if (!ctx.getUser().isInRole("HR") && !ctx.getUser().isInRole("Developer") && (msg.getRange() > maxRange)) {
			log.warn("Setting max range for {} to {} miles", env.getOwnerID(), Integer.valueOf(maxRange));
			msg.setRange(maxRange);
		}
		
		// Check that it's a valid message - if not, clear the scope center
		boolean isValid = (msg.getRange() > 2) && (msg.getLatitude() != 0.0);
		if (!isValid || !ac.getIsDispatch()) {
			log.info("Clearing radar scope for {}", ac.getUserID());
			ac.setScope(null);
		} else
			ac.setScope(msg);
		
		ctx.push(new AcknowledgeMessage(env.getOwner(), msg.getID()));
	}
}