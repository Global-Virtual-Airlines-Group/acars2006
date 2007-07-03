// Copyright 2004, 2005, 2006, 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;
import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.beans.*;
import org.deltava.beans.system.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.ConnectionMessage;

import org.deltava.dao.*;
import org.deltava.dao.acars.SetConnection;

import org.deltava.security.*;
import org.deltava.acars.security.UserBlocker;

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
	public void execute(CommandContext ctx, MessageEnvelope env) {
	   
		// Get the message and validate the user ID
		AuthenticateMessage msg = (AuthenticateMessage) env.getMessage();
		if (StringUtils.isEmpty(msg.getUserID())) {
		   AcknowledgeMessage errMsg = new AcknowledgeMessage(null, msg.getID());
		   errMsg.setEntry("error", "No User ID specified");
		   ctx.push(errMsg, env.getConnectionID());
		   return;
		}
		
		// Get the minimum build number
		int minBuild = Integer.MAX_VALUE;
		if (msg.isDispatch())
			minBuild = SystemData.getInt("acars.build.dispatch");
		else {
			Map minBuilds = (Map) SystemData.getObject("acars.build.minimum");
			if (minBuilds != null) {
				String ver = StringUtils.replace(msg.getVersion(), ".", "_");
				minBuild = StringUtils.parse((String) minBuilds.get(ver), Integer.MAX_VALUE);
			}
		}
		
		// Check the minimum build number
		if (msg.getClientBuild() < minBuild) {
		   AcknowledgeMessage errMsg = new AcknowledgeMessage(null, msg.getID());
		   if (minBuild == Integer.MAX_VALUE) {
			   errMsg.setEntry("error", "Unknown/Deprecated ACARS Client Version - " + msg.getVersion());
			   log.warn(errMsg.getEntry("error"));
		   } else
			   errMsg.setEntry("error", "Obsolete ACARS Client - Use Build " + minBuild +" or newer");
		   
		   ctx.push(errMsg, env.getConnectionID());
		   return;
		}
		
		// Get the user ID and check for valid airline code
		UserID usrID = new UserID(msg.getUserID());
		AirlineInformation aInfo = SystemData.getApp(usrID.getAirlineCode());
		if (aInfo == null)
			aInfo = SystemData.getApp(SystemData.get("airline.default"));
		
		UserData ud = null;
		Pilot usr = null;
		try {
			Connection c = ctx.getConnection(true);
			
			// Get the DAOs
			GetPilot pdao = new GetPilot(c);
			GetUserData udao = new GetUserData(c);
			
			// If we're using a database ID to authenticate, load it differently
			if (msg.isID()) {
				ud = udao.get(StringUtils.parse(msg.getUserID(), 0));
				if (ud == null)
					throw new SecurityException("Invalid database ID - " + msg.getUserID());
				
				usr = pdao.get(ud);
			} else {
				pdao.setQueryMax(1);
				usr = pdao.getPilotByCode(usrID.getUserID(), aInfo.getCode());
				if (usr != null)
					ud = udao.get(usr.getID());
			}

			// Check security access before we validate the password
			if ((usr == null) || (usr.getStatus() != Pilot.ACTIVE) || UserBlocker.isBanned(usr))
				throw new SecurityException();
			else if (msg.isDispatch() && (!usr.isInRole("Dispatch")))
				throw new SecurityException("Invalid dispatch access");
			
			// Validate the password
			Authenticator auth = (Authenticator) SystemData.getObject(SystemData.AUTHENTICATOR);
			if (auth instanceof SQLAuthenticator) {
				SQLAuthenticator sqlAuth = (SQLAuthenticator) auth;
				sqlAuth.setConnection(c);
				sqlAuth.authenticate(usr, msg.getPassword());
				sqlAuth.clearConnection();
			} else
				auth.authenticate(usr, msg.getPassword());
		} catch (SecurityException se) {
			log.warn("Authentication Failure for " + msg.getUserID());
			AcknowledgeMessage errMsg = new AcknowledgeMessage(null, msg.getID());
			if ((usr != null) && (usr.getACARSRestriction() == Pilot.ACARS_BLOCK))
				errMsg.setEntry("error", "ACARS Server access disabled");
			else if (UserBlocker.isBanned(usr))
				errMsg.setEntry("error", "ACARS Server temporary lockout");
			else if (msg.isDispatch() && (!usr.isInRole("Dispatch")))
				errMsg.setEntry("error", "Dispatch not authorized");
			else
				errMsg.setEntry("error", "Authentication Failed");
				
			ctx.push(errMsg, env.getConnectionID());
			usr = null;
		} catch (DAOException de) {
		   usr = null;
		   String errorMsg = "Error loading " + msg.getUserID() + " -  " + de.getMessage();
		   if (de.isWarning())
		      log.warn(errorMsg);
		   else
		      log.error(errorMsg, de.getLogStackDump() ? de : null);
		   
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
		if ((usr == null) || (con == null))
			return;
		
		// Calculate the difference in system times, assume 500ms latency - don't allow logins if over 4h off
		DateTime now = new DateTime(new Date());
		long timeDiff = msg.getClientUTC().getTime() - now.getUTC().getTime() + 500;
		if (Math.abs(timeDiff) > 14400000) {
			log.error("Cannot authenticate " + usr.getName() + " system clock " + (timeDiff / 1000) + " seconds off");
			AcknowledgeMessage errMsg = new AcknowledgeMessage(null, msg.getID());
			errMsg.setEntry("error", "Your system clock is " + timeDiff + " seconds off");
			ctx.push(errMsg, env.getConnectionID());
			return;
		} else if (Math.abs(timeDiff) > 900000)
			log.warn(usr.getName() + " system clock " + (timeDiff / 1000) + " seconds off");
		
		// Log the user in
		usr.login(con.getRemoteHost());
		con.setUser(usr);
		con.setUserLocation(ud);
		con.setProtocolVersion(msg.getProtocolVersion());
		con.setClientVersion(msg.getClientBuild());
		con.setIsDispatch(msg.isDispatch());
		con.setUserHidden(msg.isHidden() && usr.isInRole("HR"));
		con.setTimeOffset(timeDiff);
		
		// Save the connection data
		try {
			Connection c = ctx.getConnection();
			
			// Start a transaction
			ctx.startTX();
			
			// Get the DAO and write the connection
			SetConnection cwdao = new SetConnection(c);
			cwdao.add(con);
			
			// Save login hostname/IP address forever
			SetSystemData sysdao = new SetSystemData(c);
			sysdao.login(ud.getDB(), ud.getID(), con.getRemoteAddr(), con.getRemoteHost());
			
			// If Teamspeak is enabled, mark us as logged in
			if (SystemData.getBoolean("airline.voice.ts2.enabled")) {
				SetTS2Data ts2wdao = new SetTS2Data(c);
				ts2wdao.setActive(usr.getPilotCode(), true);
			}
			
			// Commit
			ctx.commitTX();
		} catch (DAOException de) {
			ctx.rollbackTX();
			log.error("Error logging connection", de);
		} finally {
			ctx.release();
		}

		// Tell everybody else that someone has logged on
		ConnectionMessage drMsg = new ConnectionMessage(usr, DataMessage.REQ_ADDUSER, msg.getID());
		drMsg.add(con);
		if (con.getUserHidden()) {
			for (Iterator<ACARSConnection> i  = ctx.getACARSConnectionPool().getAll().iterator(); i.hasNext(); ) {
				ACARSConnection ac = i.next();
				if ((ac.getID() != con.getID()) && ac.isAuthenticated() && ac.getUser().isInRole("HR"))
					ctx.push(drMsg, ac.getID());
			}
		} else
			ctx.pushAll(drMsg, con.getID());
		
		// If we have a newer ACARS client build, say so
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(usr, msg.getID());
		int latestBuild = SystemData.getInt("acars.build.latest");
		if (latestBuild > msg.getClientBuild())
			ackMsg.setEntry("latestBuild", String.valueOf(latestBuild));
		
		// Set roles/ratings and if we are unrestricted
		ackMsg.setEntry("userID", usr.getPilotCode());
		ackMsg.setEntry("timeOffset", String.valueOf(timeDiff / 1000));
		ackMsg.setEntry("roles", StringUtils.listConcat(usr.getRoles(), ","));
		ackMsg.setEntry("ratings", StringUtils.listConcat(usr.getRatings(), ","));
		if ((usr.getRoles().size() > 2) || (usr.getACARSRestriction() == Pilot.ACARS_OK))
			ackMsg.setEntry("unrestricted", "true");
		else if (usr.getACARSRestriction() == Pilot.ACARS_NOMSGS)
			ackMsg.setEntry("noMsgs", "true");
		
		// Send the ack message
		ctx.push(ackMsg, env.getConnectionID(), true);
		
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
		
		// Add hidden notice
		if (con.getUserHidden())
			sysMsg.addMessage("You are in STEALTH mode and do not appear in the connections list.");
		
		// Send the message
		ctx.push(sysMsg, env.getConnectionID());
		
		// Log new connection
		log.info("New Connection from " + usr.getName() + " (Build " + con.getClientVersion() + ")");
	}
	
	/**
	 * Returns the maximum execution time of this command before a warning is issued.
	 * @return the maximum execution time in milliseconds
	 */
	public final int getMaxExecTime() {
		return 3000;
	}
}