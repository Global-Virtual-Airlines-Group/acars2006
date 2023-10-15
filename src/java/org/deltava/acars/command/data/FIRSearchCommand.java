// Copyright 2014, 2019, 2020, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;

import org.deltava.beans.GeoLocation;
import org.deltava.beans.navdata.FIR;
import org.deltava.beans.schedule.GeoPosition;

import org.deltava.dao.*;

import org.deltava.util.StringUtils;

/**
 * An ACARS data command to find the current Flight Infromation Region.
 * @author Luke
 * @version 11.1
 * @since 6.1
 */

public class FIRSearchCommand extends DataCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		ACARSConnection ac = ctx.getACARSConnection();
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());

		// Get the location
		GeoLocation loc = ac.getPosition();
		if (msg.hasFlag("lat"))
			loc = new GeoPosition(StringUtils.parse(msg.getFlag("lat"), 0d), StringUtils.parse(msg.getFlag("lng"), 0d));
		
		if (loc == null) return;
		try {
			GetFIR fdao = new GetFIR(ctx.getConnection());
			FIR f = fdao.search(loc);
			if (f != null) {
				ackMsg.setEntry("id", f.getID());
				ackMsg.setEntry("name", f.getName());
				ackMsg.setEntry("oceanic", String.valueOf(f.isOceanic()));
				ackMsg.setEntry("aux", String.valueOf(f.isAux()));
			}
		} catch (DAOException de) {
			log.atError().withThrowable(de).log("Error searching FIR data - {}", de.getMessage());
			ackMsg.setEntry("error", "Cannot determine FIR - " + de.getMessage());
		} finally {
			ctx.release();
		}

		ctx.push(ackMsg);
	}
}