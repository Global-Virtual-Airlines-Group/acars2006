// Copyright 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.command;

import java.util.Date;
import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import org.deltava.dao.acars.SetInfo;
import org.deltava.dao.DAOException;

import org.deltava.util.StringUtils;

/**
 * An ACARS Command to log the completion of a flight.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class EndFlightCommand implements ACARSCommand {

	private static final Logger log = Logger.getLogger(EndFlightCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, Envelope env) {

		// Get the message
		ACARSConnection con = ctx.getACARSConnection();

		// Get the current info
		InfoMessage iMsg = con.getFlightInfo();
		if (iMsg == null) {
			log.warn("No Flight Information for Connection " + StringUtils.formatHex(con.getID()));
			return;
		}

		// Write the info to the database
		iMsg.setEndTime(new Date());
		try {
			Connection c = ctx.getConnection();
			SetInfo infoDAO = new SetInfo(c);
			infoDAO.close(iMsg.getFlightID(), env.getConnectionID(), true);
		} catch (DAOException de) {
			log.error(de.getMessage(), de);
		} finally {
			ctx.release();
		}

		// Clear flight info and log
		log.info("Flight Completed by " + con.getUserID());
	}
}