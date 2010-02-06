// Copyright 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import org.deltava.dao.DAOException;
import org.deltava.dao.acars.SetTakeoff;

/**
 * An ACARS command to process takeoff/touchdown messages.
 * @author Luke
 * @version 2.8
 * @since 2.8
 */

public class TakeoffCommand extends ACARSCommand {
	
	private static final Logger log = Logger.getLogger(TakeoffCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the message
		TakeoffMessage msg = (TakeoffMessage) env.getMessage();
		ACARSConnection ac = ctx.getACARSConnection();
		InfoMessage info = ac.getFlightInfo();
		if (info == null) {
			log.error(ac.getUserID() + " sending Takeoff message - no Flight Info found");
			return;
		}
		
		// Copy message data
		msg.setAirportD(info.getAirportD());
		msg.setAirportA(info.getAirportA());
		msg.setEquipmentType(info.getEquipmentType());
		msg.setFlightCode(info.getFlightCode());
		
		// Log the message
		boolean isBounce = false;
		try {
			SetTakeoff todao = new SetTakeoff(ctx.getConnection());
			isBounce = todao.logTakeoff(info.getFlightID(), msg.isTakeoff());
		} catch (DAOException de) {
			log.error("Cannnot log takeoff/landing - " + de.getMessage(), de);
		} finally {
			ctx.release();
		}
		
		// Send out a system message to the others if not a bounce
		if (!isBounce)
			ctx.pushAll(msg, ac.getID());
	}
}