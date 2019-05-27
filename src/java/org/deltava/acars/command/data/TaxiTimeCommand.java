// Copyright 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;

import org.deltava.beans.schedule.Airport;

import org.deltava.dao.*;

import org.deltava.util.system.SystemData;

/**
 * An ACARS command to return airport taxi times.
 * @author Luke
 * @version 8.6
 * @since 8.6
 */

public class TaxiTimeCommand extends DataCommand {

	/**
	 * Initializes the Command.
	 */
	public TaxiTimeCommand() {
		super(TaxiTimeCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		
		boolean isTakeoff = Boolean.valueOf(msg.getFlag("isTakeoff")).booleanValue();
		Airport a = SystemData.getAirport(msg.getFlag("airport"));
		if (a == null) {
			ctx.push(new AcknowledgeMessage(env.getOwner(), msg.getID(), "Unknown Airport - " + msg.getFlag("airport")));
			return;
		}
		
		String db = ctx.getACARSConnection().getUserData().getDB();
		try {
			GetACARSTaxiTimes ttdao = new GetACARSTaxiTimes(ctx.getConnection());
			int taxiTime = isTakeoff ? ttdao.getTaxiOutTime(a, db) : ttdao.getTaxiInTime(a, db);
			AcknowledgeMessage ackMsg = 	new AcknowledgeMessage(env.getOwner(), msg.getID());
			ackMsg.setEntry("taxiTime", String.valueOf(taxiTime));
			ctx.push(ackMsg);
		} catch (DAOException de) {
			log.error("Error loading taxi times - " + de.getMessage(), de);
		} finally {
			ctx.release();
		}
	}
}