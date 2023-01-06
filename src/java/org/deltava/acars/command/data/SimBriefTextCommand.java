// Copyright 2022, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;

import org.deltava.beans.simbrief.BriefingPackage;

import org.deltava.dao.*;
import org.deltava.util.StringUtils;

/**
 * An ACARS Command to return SimBrief briefing packages.
 * @author Luke
 * @version 10.4
 * @since 10.3
 */

public class SimBriefTextCommand extends DataCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		int id = StringUtils.parse(msg.getFlag("id"), 0);

		try {
			GetSimBriefPackages sbdao = new GetSimBriefPackages(ctx.getConnection());
			BriefingPackage pkg = sbdao.getSimBrief(id, ctx.getDB());
			if (pkg == null) {
				ctx.push(new AcknowledgeMessage(env.getOwner(), msg.getID(), "Unknown Flight Report - " + id));
				return;
			}
			
			AcknowledgeMessage ackMsg = 	new AcknowledgeMessage(env.getOwner(), msg.getID());
			ackMsg.setEntry("pkg", pkg.getBriefingText());
			ctx.push(ackMsg);
		} catch (DAOException de) {
			log.error("Error loading briefing package - " + de.getMessage(), de);
		} finally {
			ctx.release();
		}
	}
}