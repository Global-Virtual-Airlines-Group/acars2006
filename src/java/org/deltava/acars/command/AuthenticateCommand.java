// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.command;

import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.beans.Pilot;
import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import org.deltava.acars.xml.MessageWriter;
import org.deltava.acars.xml.XMLException;

import org.deltava.dao.DAOException;
import org.deltava.dao.GetPilot;
import org.deltava.dao.acars.SetConnection;

import org.deltava.security.Authenticator;

import org.deltava.util.UserID;
import org.deltava.util.system.SystemData;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class AuthenticateCommand implements ACARSCommand {

	private static final Logger log = Logger.getLogger(AuthenticateCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, Envelope env) {

		// Get the message and the user ID
		AuthenticateMessage msg = (AuthenticateMessage) env.getMessage();
		UserID usrID = new UserID(msg.getUserID());

		Pilot usr = null;
		try {
			Connection c = ctx.getConnection();

			// Figure out the DN from the Pilot ID
			GetPilot pdao = new GetPilot(c);
			usr = pdao.getPilotByCode(usrID.getUserID(), usrID.getAirlineCode());
			if ((usr == null) || (usr.getStatus() != Pilot.ACTIVE))
				throw new SecurityException("Unknown User ID");

			// Validate the password
			Authenticator auth = (Authenticator) SystemData.getObject(SystemData.AUTHENTICATOR);
			auth.authenticate(usr.getDN(), msg.getPassword());
		} catch (SecurityException se) {
			usr = null;
			log.warn("Authentication Failure for " + msg.getUserID());
			AcknowledgeMessage errMsg = new AcknowledgeMessage(usr, msg.getID());
			errMsg.setEntry("error", "Authentication Failed");
			ctx.push(errMsg, env.getConnectionID());
		} catch (DAOException de) {
			log.error("Error loading " + msg.getUserID() + " - " + de.getMessage());
			AcknowledgeMessage errMsg = new AcknowledgeMessage(usr, msg.getID());
			errMsg.setEntry("error", "Authentication Failed");
			ctx.push(errMsg, env.getConnectionID());
		} finally {
			ctx.release();
		}

		// If we're not logged in, abort
		if (usr == null)
			return;

		// Get the connection
		ACARSConnection con = ctx.getACARSConnection();

		// Log the user in
		usr.login(con.getRemoteHost());
		con.setUser(usr);
		con.setProtocolVersion(msg.getProtocolVersion());

		// Update the registration with the dispatcher - remove and re-register
		try {
			MessageWriter.remove(con.getID());
			MessageWriter.addConnection(con.getID(), usr, con.getProtocolVersion());
		} catch (XMLException xe) {
			log.error("Cannot re-register " + con.getFormatID() + " - " + xe.getMessage());
		}
		
		// Save the connection data
		try {
			Connection c = ctx.getConnection();
			
			// Get the DAO and write the connection
			SetConnection cwdao = new SetConnection(c);
			cwdao.add(con);
		} catch (DAOException de) {
			log.error("Error logging connection", de);
		} finally {
			ctx.release();
		}

		// Tell everybody else that someone has logged on
		DataRequestMessage drMsg = new DataRequestMessage(null, DataRequestMessage.REQ_ADDUSER);
		drMsg.setFilter(con.getFormatID());
		ctx.pushAll(drMsg, con.getID());

		// Send the ack message
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(usr, msg.getID());
		ctx.push(ackMsg, env.getConnectionID());
	}
}