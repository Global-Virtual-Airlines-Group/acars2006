// Copyright 2009, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.beans.navdata.Runway;
import org.deltava.beans.schedule.ICAOAirport;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import org.deltava.dao.*;
import org.deltava.dao.acars.SetTakeoff;

import org.deltava.util.GeoUtils;

/**
 * An ACARS command to process takeoff/touchdown messages.
 * @author Luke
 * @version 4.1
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
		ICAOAirport a = msg.isTakeoff() ? info.getAirportD() : info.getAirportA();
		try {
			Connection con = ctx.getConnection();
			SetTakeoff todao = new SetTakeoff(con);
			isBounce = todao.logTakeoff(info.getFlightID(), msg.isTakeoff());

			// Get the runway
			GetNavData nvdao = new GetNavData(con);
			Runway r = nvdao.getBestRunway(a, 0, msg.getLocation(), msg.getHeading());
			if ((r != null) && !isBounce) {
				int dist = GeoUtils.distanceFeet(r, msg.getLocation());
				
				// Send the ack
				AcknowledgeMessage ackMsg = new AcknowledgeMessage(msg.getSender(), msg.getID());
				ackMsg.setEntry("rwy", r.getName());
				ackMsg.setEntry("distance", String.valueOf(dist));
				ackMsg.setEntry("takeoff", String.valueOf(msg.isTakeoff()));
				ctx.push(ackMsg, ac.getID());
			}
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