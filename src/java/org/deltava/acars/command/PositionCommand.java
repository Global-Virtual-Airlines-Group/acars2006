// Copyright 2004,2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;
import org.deltava.beans.acars.ACARSFlags;

import org.deltava.acars.util.MessageCache;

import org.deltava.util.system.SystemData;

/**
 * An ACARS server command to process position updates.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class PositionCommand extends ACARSCommand {
	
	public static final MessageCache<PositionMessage> CACHE = new MessageCache<PositionMessage>(10, 10000);

	private static final Logger log = Logger.getLogger(PositionCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, Envelope env) {

		// Get the Message and the ACARS Connection
		PositionMessage msg = (PositionMessage) env.getMessage();
		ACARSConnection con = ctx.getACARSConnection();
		if (con == null) {
			log.warn("Missing Connection for " + env.getOwnerID());
			return;
		} else if (con.getIsDispatch()) {
			log.warn("Dispatch Client sending Position Report!");
			return;
		}

		// Create the ack message and envelope
		AcknowledgeMessage ackMsg = null;
		if (SystemData.getBoolean("acars.ack.position"))
			ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());

		// Get the last position report and its age
		InfoMessage info = con.getFlightInfo();
		PositionMessage oldPM = con.getPosition();
		if (info == null) {
			log.warn("No Flight Information for " + con.getUserID());
			if (ackMsg != null)
				ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			
			ackMsg.setEntry("sendInfo", "true");
			ctx.push(ackMsg, env.getConnectionID());
			return;
		}

		// If we are an offline fight, save the flight right away
		if (msg.getNoFlood()) {
			CACHE.push(msg, con.getID(), con.getFlightID());
		} else {
			// Check for position flood
			long pmAge = System.currentTimeMillis() - ((oldPM == null) ? 0 : oldPM.getTime());
			if (pmAge >= SystemData.getInt("acars.position_interval")) {
				if (!msg.isFlagSet(ACARSFlags.FLAG_PAUSED)) {
					con.setPosition(msg);
					if (msg.isLogged())
						CACHE.push(msg, con.getID(), con.getFlightID());
				} else {
					con.setPosition(null);					
				}
			} else {
				log.warn("Position flood from " + con.getUser().getName() + " (" + con.getUserID() + "), interval="  + pmAge + "ms");
				return;
			}
		}

		// Log message received
		log.debug("Received position from " + con.getUser().getPilotCode());
		if (ackMsg != null)
			ctx.push(ackMsg, env.getConnectionID());
	}
}