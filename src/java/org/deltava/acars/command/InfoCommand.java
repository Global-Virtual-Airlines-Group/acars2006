// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.command;

import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import org.deltava.dao.DAOException;
import org.deltava.dao.acars.SetInfo;

/**
 * An ACARS Command to log Flight data.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class InfoCommand implements ACARSCommand {

	private static final Logger log = Logger.getLogger(InfoCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, Envelope env) {

		// Get the message
		InfoMessage msg = (InfoMessage) env.getMessage();
		String flightType = msg.isOffline() ? "Offline" : "Online";
		boolean assignID = (msg.getFlightID() == 0);

		// Write the info to the database
		try {
			Connection c = ctx.getConnection();
			SetInfo infoDAO = new SetInfo(c);
			infoDAO.write(msg, env.getConnectionID());
			
			// Log returned flight id
			if (assignID)
				log.info("Assigned " + flightType + " Flight ID " + String.valueOf(msg.getFlightID()));

			// Create the ack message and envelope - these are always acknowledged
			AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			ackMsg.setEntry("flight_id", String.valueOf(msg.getFlightID()));
			ctx.push(ackMsg, env.getConnectionID());
		} catch (DAOException de) {
			log.error(de.getMessage(), de);
		} finally {
			ctx.release();
		}

		// Set the info for the connection and write it to the database
		ACARSConnection con = ctx.getACARSConnection();
		con.setInfo(msg);
		if (msg.isComplete()) {
			log.info("Received completed " + flightType + " flight information from " + con.getUserID());
		} else {
			log.info("Received " + flightType + " flight information from " + con.getUserID());
		}
	}
}