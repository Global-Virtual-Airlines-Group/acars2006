// Copyright 2004, 2005, 2006, 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;
import org.deltava.beans.acars.ACARSFlags;

import org.deltava.dao.DAOException;
import org.deltava.dao.acars.SetPosition;

import org.deltava.util.CalendarUtils;
import org.deltava.util.system.SystemData;

/**
 * An ACARS server command to process position updates.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class PositionCommand extends ACARSCommand {

	private static final Logger log = Logger.getLogger(PositionCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the Message and the ACARS Connection
		PositionMessage msg = (PositionMessage) env.getMessage();
		ACARSConnection ac = ctx.getACARSConnection();
		if (ac == null) {
			log.warn("Missing Connection for " + env.getOwnerID());
			return;
		} else if (ac.getIsDispatch()) {
			log.warn("Dispatch Client sending Position Report!");
			return;
		}

		// Create the ack message and envelope
		AcknowledgeMessage ackMsg = null;
		if (SystemData.getBoolean("acars.ack.position"))
			ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());

		// Get the last position report and its age
		InfoMessage info = ac.getFlightInfo();
		PositionMessage oldPM = ac.getPosition();
		if (info == null) {
			log.warn("No Flight Information for " + ac.getUserID());
			if (ackMsg != null)
				ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());

			ackMsg.setEntry("sendInfo", "true");
			ctx.push(ackMsg, env.getConnectionID());
			return;
		}
		
		// Adjust the message date
		msg.setDate(CalendarUtils.adjustMS(msg.getDate(), ac.getTimeOffset()));

		// Calculate the age of the last message
		long pmAge = System.currentTimeMillis() - ((oldPM == null) ? 0 : oldPM.getTime());

		// Queue it up
		if (msg.getNoFlood())
			SetPosition.queue(msg, ac.getID(), ac.getFlightID());
		else {
			// Check for position flood
			if (pmAge >= SystemData.getInt("acars.position_interval")) {
				if (!msg.isFlagSet(ACARSFlags.FLAG_PAUSED)) {
					ac.setPosition(msg);
					if (msg.isLogged())
						SetPosition.queue(msg, ac.getID(), ac.getFlightID());
				} else
					ac.setPosition(null);
			} else if (!msg.isLogged())
				ac.setPosition(msg);
			else {
				log.warn("Position flood from " + ac.getUser().getName() + " (" + ac.getUserID() + "), interval="
						+ pmAge + "ms");
				return;
			}
		}

		// Check if the cache needs to be flushed
		synchronized (SetPosition.class) {
			if (SetPosition.getMaxAge() > 45000) {
				try {
					Connection con = ctx.getConnection(true);
					SetPosition dao = new SetPosition(con);
					int entries = dao.flush();
					log.info("Flushed " + entries + " cached position entries");
				} catch (DAOException de) {
					log.error("Error flushing positions - " + de.getMessage(), de);
				} finally {
					ctx.release();
				}
			}
		}

		// Log message received
		ctx.push(ackMsg, env.getConnectionID());
		if (log.isDebugEnabled())
			log.debug("Received position from " + ac.getUser().getPilotCode());
	}
}