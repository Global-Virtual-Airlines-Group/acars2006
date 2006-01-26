// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.command;

import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;
import org.deltava.beans.acars.FlightInfo;
import org.deltava.beans.system.UserData;
import org.deltava.beans.testing.CheckRide;
import org.deltava.beans.testing.Test;

import org.deltava.dao.*;
import org.deltava.dao.acars.SetInfo;

/**
 * An ACARS Command to log Flight data.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class InfoCommand extends ACARSCommand {

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
		
		// Check if we already have a flight ID and are requesting a new one
		boolean assignID = (msg.getFlightID() == 0);
		ACARSConnection con = ctx.getACARSConnection();
		InfoMessage curInfo = con.getFlightInfo();
		UserData usrLoc = con.getUserData();
		
		// Check for a duplicate Flight ID request
		if (assignID && (curInfo != null) && (curInfo.getFlightID() != 0) && (!curInfo.isComplete())) {
		   msg.setFlightID(curInfo.getFlightID());
		   log.warn("Duplicate Flight ID request - assigning Flight ID " + msg.getFlightID());

		   // Send back the acknowledgement
			AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			ackMsg.setEntry("flight_id", String.valueOf(msg.getFlightID()));
			ctx.push(ackMsg, env.getConnectionID());
			return;
		}

		// Write the info to the database
		try {
			Connection c = ctx.getConnection();
			
			// If we're requesting a specific ID, make sure we used to own it
			if (!assignID) {
			   GetACARSData rdao = new GetACARSData(c);
			   FlightInfo info = rdao.getInfo(msg.getFlightID());
			   if (info == null) {
			      log.warn(env.getOwnerID() + " requesting invalid Flight " + msg.getFlightID());
			      assignID = true;
			      msg.setFlightID(0);
			   } else if (info.getPilotID() != env.getOwner().getID()) {
			      log.warn(env.getOwnerID() + " requesting owned Flight " + msg.getFlightID());
			      assignID = true;
			      msg.setFlightID(0);
			   }
			}
			
			// Look for a checkride record
			GetExam exdao = new GetExam(c);
			CheckRide cr = exdao.getCheckRide(usrLoc.getDB(), usrLoc.getID(), msg.getEquipmentType(), Test.NEW);
			if (cr != null)
				msg.setCheckRide(true);
			
			// Write the flight information
			SetInfo infoDAO = new SetInfo(c);
			infoDAO.write(msg, env.getConnectionID());
		} catch (DAOException de) {
			log.error(de.getMessage(), de);
		} finally {
			ctx.release();
		}
		
		// Log returned flight id
		if (assignID) {
			log.info("Assigned " + flightType + " Flight ID " + String.valueOf(msg.getFlightID()));
		} else {
		   log.info(env.getOwnerID() + " resuming Flight " + msg.getFlightID());
		}

		// Create the ack message and envelope - these are always acknowledged
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
		ackMsg.setEntry("flight_id", String.valueOf(msg.getFlightID()));
		ackMsg.setEntry("checkRide", String.valueOf(msg.isCheckRide()));
		ctx.push(ackMsg, env.getConnectionID());

		// Set the info for the connection and write it to the database
		con.setFlightInfo(msg);
		if (msg.isComplete()) {
			log.info("Received completed " + flightType + " flight information from " + con.getUserID());
		} else {
			log.info("Received " + flightType + " flight information from " + con.getUserID());
		}
	}
}