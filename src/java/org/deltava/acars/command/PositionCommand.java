// Copyright 2004, 2005, 2006, 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;
import org.deltava.beans.acars.ACARSFlags;

import org.deltava.dao.DAOException;
import org.deltava.dao.acars.SetPosition;

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

		// Write to the database
		try {
			Connection c = ctx.getConnection(true);
			SetPosition dao = new SetPosition(c);
			if (msg.getNoFlood()) {
				dao.write(msg, con.getID(), con.getFlightID());
			} else {
				// Check for position flood
				long pmAge = System.currentTimeMillis() - ((oldPM == null) ? 0 : oldPM.getTime());
				if (pmAge >= SystemData.getInt("acars.position_interval")) {
					if (!msg.isFlagSet(ACARSFlags.FLAG_PAUSED)) {
						con.setPosition(msg);
						if (msg.isLogged())
							dao.write(msg, con.getID(), con.getFlightID());
					} else
						con.setPosition(null);
				} else if (!msg.isLogged())
					con.setPosition(msg);
				else {
					log.warn("Position flood from " + con.getUser().getName() + " (" + con.getUserID() + "), interval="
							+ pmAge + "ms");
					return;
				}
			}
		} catch (DAOException de) {
			log.error("Error writing position - " + de.getMessage(), de);
		} finally {
			ctx.release();
		}

		// Log message received
		ctx.push(ackMsg, env.getConnectionID());
		if (log.isDebugEnabled())
			log.debug("Received position from " + con.getUser().getPilotCode());
	}
}