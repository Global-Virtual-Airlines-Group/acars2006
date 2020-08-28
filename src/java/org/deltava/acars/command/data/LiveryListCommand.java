// Copyright 2008, 2016, 2019, 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;

import org.deltava.beans.acars.Livery;

import org.deltava.dao.*;

import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.LiveryMessage;

import org.deltava.util.system.SystemData;

/**
 * An ACARS data command to return available multi-player livery data.
 * @author Luke
 * @version 9.1
 * @since 2.2
 */

public class LiveryListCommand extends DataCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		try {
			GetACARSLivery dao = new GetACARSLivery(ctx.getConnection());
			Collection<Livery> results = dao.get(SystemData.getAirline(msg.getFlag("airline")));
			
			// Send the message
			LiveryMessage lmsg = new LiveryMessage(env.getOwner(), msg.getID());
			lmsg.addAll(results);
			ctx.push(lmsg);
		} catch (DAOException de) {
			log.error("Error loading MP liveries - " + de.getMessage(), de);
			ctx.push(new AcknowledgeMessage(env.getOwner(), msg.getID(), "Cannot load liveries - " + de.getMessage()));
		} finally {
			ctx.release();
		}
	}
}