// Copyright (c) 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.Collection;
import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.beans.Pilot;
import org.deltava.beans.acars.ServerStats;
import org.deltava.beans.system.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;
import org.deltava.acars.xml.*;

import org.deltava.dao.*;
import org.deltava.dao.acars.SetConnection;

import org.deltava.jdbc.ConnectionPoolFullException;

import org.deltava.security.Authenticator;

import org.deltava.util.*;
import org.deltava.util.system.SystemData;

/**
 * An ACARS server command to authenticate a user.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class AuthenticateCommand extends ACARSCommand {

	private static final Logger log = Logger.getLogger(AuthenticateCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, Envelope env) {
	   
		// Get the message and validate the user ID
		AuthenticateMessage msg = (AuthenticateMessage) env.getMessage();
		if (StringUtils.isEmpty(msg.getUserID())) {
		   AcknowledgeMessage errMsg = new AcknowledgeMessage(null, msg.getID());
		   errMsg.setEntry("error", "No User ID specified");
		   ctx.push(errMsg, env.getConnectionID());
		   return;
		}
		
		// Check the minimum build number
		int minBuild = SystemData.getInt("acars.build.minimum");
		if (msg.getClientBuild() < minBuild) {
		   AcknowledgeMessage errMsg = new AcknowledgeMessage(null, msg.getID());
		   errMsg.setEntry("error", "Obsolete Build - Use Build " + minBuild +" or newer");
		   ctx.push(errMsg, env.getConnectionID());
		   return;
		}
		
		// Get the user ID and check for valid airline code
		UserID usrID = new UserID(msg.getUserID());
		AirlineInformation aInfo = SystemData.getApp(usrID.getAirlineCode());
		if (aInfo == null)
			aInfo = SystemData.getApp(SystemData.get("airline.code"));
		
		UserData ud = null;
		Pilot usr = null;
		try {
			Connection c = ctx.getConnection(true);

			// Figure out the DN from the Pilot ID
			GetPilot pdao = new GetPilot(c);
			pdao.setQueryMax(1);
			usr = pdao.getPilotByCode(usrID.getUserID(), aInfo.getCode());
			if ((usr == null) || (usr.getStatus() != Pilot.ACTIVE))
				throw new SecurityException("Unknown User ID");
			
			// Get the User location data
			GetUserData udao = new GetUserData(c);
			ud = udao.get(usr.getID());

			// Validate the password
			Authenticator auth = (Authenticator) SystemData.getObject(SystemData.AUTHENTICATOR);
			auth.authenticate(usr, msg.getPassword());
		} catch (SecurityException se) {
			log.warn("Authentication Failure for " + msg.getUserID());
			AcknowledgeMessage errMsg = new AcknowledgeMessage(null, msg.getID());
			if ((usr != null) && usr.getNoACARS()) {
				errMsg.setEntry("error", "ACARS Server access disabled");
			} else {
				errMsg.setEntry("error", "Authentication Failed");
			}
				
			ctx.push(errMsg, env.getConnectionID());
			usr = null;
		} catch (DAOException de) {
		   usr = null;
		   if (de instanceof ConnectionPoolFullException) {
		      log.warn("Error loading " + msg.getUserID() + " - Connection Pool Full");
		   } else {
		      log.error("Error loading " + msg.getUserID() + " - " + de.getMessage(), de);
		   }
		   
			AcknowledgeMessage errMsg = new AcknowledgeMessage(null, msg.getID());
			errMsg.setEntry("error", "Authentication Failed - " + de.getMessage());
			ctx.push(errMsg, env.getConnectionID());
		} catch (Exception e) {
		   usr = null;
			log.error("Error loading " + msg.getUserID() + " - " + e.getMessage(), e);
			AcknowledgeMessage errMsg = new AcknowledgeMessage(null, msg.getID());
			errMsg.setEntry("error", "Authentication Failed - " + e.getMessage());
			ctx.push(errMsg, env.getConnectionID());
		} finally {
			ctx.release();
		}
		
		// Get the ACARS connection
		ACARSConnection con = ctx.getACARSConnection();

		// If we're not logged in or the connection is gone, abort
		if ((usr == null) || (con == null))
			return;

		// Log the user in
		usr.login(con.getRemoteHost());
		con.setUser(usr);
		con.setUserLocation(ud);
		con.setProtocolVersion(msg.getProtocolVersion());
		con.setClientVersion(msg.getClientBuild());
		
		// Log successful authentication
		ServerStats.authenticate();

		// Update the registration with the dispatcher - remove and re-register
		try {
			MessageWriter.remove(con.getID());
			MessageWriter.addConnection(con.getID(), usr, con.getProtocolVersion());
		} catch (XMLException xe) {
			log.error("Cannot re-register " + StringUtils.formatHex(con.getID()) + " - " + xe.getMessage());
		}
		
		// Save the connection data
		try {
			Connection c = ctx.getConnection();
			
			// Get the DAO and write the connection
			SetConnection cwdao = new SetConnection(c);
			cwdao.add(con);
			
			// If Teamspeak is enabled, mark us as logged in
			if (SystemData.getBoolean("airline.voice.ts2.enabled")) {
				SetTS2Data ts2wdao = new SetTS2Data(c);
				ts2wdao.setActive(usr.getPilotCode(), true);
			}
		} catch (DAOException de) {
			log.error("Error logging connection", de);
		} finally {
			ctx.release();
		}

		// Tell everybody else that someone has logged on
		DataResponseMessage drMsg = new DataResponseMessage(usr, DataMessage.REQ_ADDUSER);
		drMsg.addResponse(con);
		ctx.pushAll(drMsg, con.getID());
		
		// If we have a newer ACARS client build, say so
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(usr, msg.getID());
		int latestBuild = SystemData.getInt("acars.build.latest");
		if (latestBuild > msg.getClientBuild())
			ackMsg.setEntry("latestBuild", String.valueOf(latestBuild));
		
		// Set roles
		ackMsg.setEntry("roles", StringUtils.listConcat(usr.getRoles(), ","));

		// Send the ack message
		ctx.push(ackMsg, env.getConnectionID());
		
		// Return a system message to the user
		SystemTextMessage sysMsg = new SystemTextMessage();
		sysMsg.addMessage("Welcome to the " + SystemData.get("airline.name") + " ACARS server! (Build " + VersionInfo.BUILD + ")");
		sysMsg.addMessage(VersionInfo.TXT_COPYRIGHT);
		sysMsg.addMessage("You are logged in as " + usr.getName() + " (" + usr.getPilotCode() + ") from " + con.getRemoteAddr());
		
		// Add system-defined messages
		@SuppressWarnings("unchecked")
		Collection<? extends String> systemMsgs = (Collection<? extends String>) SystemData.getObject("acars.login_msgs");
		if (systemMsgs != null)
			sysMsg.addMessages(systemMsgs);
		
		// Send the message
		ctx.push(sysMsg, env.getConnectionID());
		
		// Log new connection
		log.info("New Connection from " + usr.getName() + " (Build " + con.getClientVersion() + ")");
	}
	
	/**
	 * Returns the maximum execution time of this command before a warning is issued.
	 * @return the maximum execution time in milliseconds
	 */
	@Override
	public final int getMaxExecTime() {
		return 3000;
	}
}