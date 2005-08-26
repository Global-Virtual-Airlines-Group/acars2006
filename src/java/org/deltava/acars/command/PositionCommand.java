// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.command;

import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import org.deltava.dao.DAOException;
import org.deltava.dao.acars.SetPosition;

import org.deltava.util.StringUtils;
import org.deltava.util.system.SystemData;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class PositionCommand implements ACARSCommand {
	
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

		// Create the ack message and envelope
		if (SystemData.getBoolean("acars.ack.position")) {
			AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			ctx.push(ackMsg, con.getID());
		}

		// Get the last position report and its age
		InfoMessage info = con.getFlightInfo();
		PositionMessage oldPM = con.getPosition();
		if (info == null)
		   return;
		
		// If we are an offline fight, update the timestamp of the mesage
		if (info.isOffline() && info.isComplete()) {
			msg.setTime(msg.getDate().getTime());
			info.addPosition(msg);
		} else {
		   // Check for position flood
		   long pmAge = System.currentTimeMillis() - ((oldPM == null) ? 0 : oldPM.getTime());
		   if (pmAge >= SystemData.getInt("acars.position_interval")) {
				try {
					Connection c = ctx.getConnection();
					SetPosition dao = new SetPosition(c);
					dao.write(msg, con.getID(), con.getFlightID());
				} catch (DAOException de) {
					log.error(de.getMessage(), de);
				} finally {
					ctx.release();
				}
		   } else {
		      log.warn("Position flood from " + con.getUser().getName() + " (" + con.getUserID() + "), interval=" + String.valueOf(pmAge) + "ms");
		      return;
		   }
		}
		
		// Log and update
		log.debug("Received position from " + con.getUser().getPilotCode() + " (" + StringUtils.formatHex(con.getID()) + ")");
	   con.setInfo(msg);
	}
}