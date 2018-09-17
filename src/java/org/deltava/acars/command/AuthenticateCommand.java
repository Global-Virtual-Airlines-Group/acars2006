// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.net.*;
import java.sql.*;
import java.util.*;
import java.time.Instant;
import java.time.ZonedDateTime;

import org.apache.log4j.Logger;

import org.deltava.beans.*;
import org.deltava.beans.acars.*;
import org.deltava.beans.stats.SystemInformation;
import org.deltava.beans.system.*;

import org.deltava.acars.ACARSException;
import org.deltava.acars.beans.*;

import org.deltava.acars.message.*;
import org.deltava.acars.message.data.ConnectionMessage;

import org.deltava.dao.*;
import org.deltava.dao.acars.SetConnection;

import org.deltava.security.*;

import org.deltava.util.*;
import org.deltava.util.system.SystemData;

/**
 * An ACARS server command to authenticate a user.
 * @author Luke
 * @version 8.4
 * @since 1.0
 */

public class AuthenticateCommand extends ACARSCommand {

	private static final Logger log = Logger.getLogger(AuthenticateCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the message and validate the user ID
		final AuthenticateMessage msg = (AuthenticateMessage) env.getMessage();
		if (StringUtils.isEmpty(msg.getUserID())) {
			AcknowledgeMessage errMsg = new AcknowledgeMessage(null, msg.getID());
			errMsg.setEntry("error", "No User ID specified");
			ctx.push(errMsg, env.getConnectionID());
			return;
		}

		// Get the user ID and check for valid airline code
		UserID usrID = new UserID(msg.getUserID());
		AirlineInformation aInfo = SystemData.getApp(usrID.getAirlineCode());
		if (aInfo == null)
			aInfo = SystemData.getApp(SystemData.get("airline.default"));

		ClientInfo cInfo = msg.getClientInfo();
		ClientInfo latestClient = null; UserData ud = null; Pilot usr = null;
		try {
			Connection c = ctx.getConnection();
			
			// Get the minimum build number
			GetACARSBuilds abdao = new GetACARSBuilds(c);
			latestClient = abdao.getLatestBuild(cInfo);
			boolean isOK = abdao.isValid(cInfo);
			if (!isOK || (latestClient == null)) {
				String ct = cInfo.getClientType().getName();
				if (latestClient == null)
					throw new ACARSException("Unknown/Deprecated ACARS " + ct + " Version - " + cInfo.getVersion());	
				else if (cInfo.isBeta())
					throw new ACARSException("Unknown/Deprecated ACARS beta - Build " + cInfo.getClientBuild() + " Beta " + cInfo.getBeta());	
				
				throw new ACARSException("Obsolete ACARS " + ct + " Client - Use Build " + latestClient.getClientBuild() + " or newer");	
			}

			// Get the DAOs
			GetPilot pdao = new GetPilot(c);
			GetUserData udao = new GetUserData(c);

			// If we're using a database ID to authenticate, load it differently
			if (!usrID.hasAirlineCode()) {
				ud = udao.get(StringUtils.parse(msg.getUserID(), 0));
				if (ud == null)
					throw new SecurityException("Invalid database ID - " + msg.getUserID());

				usr = pdao.get(ud);
			} else {
				usr = pdao.getPilotByCode(usrID.getUserID(), aInfo.getDB());
				if (usr != null)
					ud = udao.get(usr.getID());
			}

			// Check security access before we validate the password
			if ((usr == null) || (usr.getStatus() != Pilot.ACTIVE) || (usr.getACARSRestriction() == Restriction.BLOCK))
				throw new SecurityException();
			else if (cInfo.isDispatch() && (!usr.isInRole("Dispatch")))
				throw new SecurityException("Invalid dispatch access");
			else if (ud == null)
				throw new SecurityException("Cannot load user data - " + msg.getUserID());
			
			// Validate the password
			org.deltava.security.Authenticator auth = (org.deltava.security.Authenticator) SystemData.getObject(SystemData.AUTHENTICATOR);
			if (auth instanceof SQLAuthenticator) {
				try (SQLAuthenticator sqlAuth = (SQLAuthenticator) auth) {
					sqlAuth.setConnection(c);
					sqlAuth.authenticate(usr, msg.getPassword());
				}
			} else
				auth.authenticate(usr, msg.getPassword());
			
			// Check if we're already logged in
			ACARSConnection ac2 = ctx.getACARSConnection(usr.getPilotCode());
			if (ac2 != null) {
				String code = StringUtils.isEmpty(usr.getPilotCode()) ? usr.getName() : usr.getPilotCode();
				String remoteAddr = ctx.getACARSConnection().getRemoteAddr();
				boolean isDSP = (cInfo.isDispatch() || ac2.getIsDispatch());
				if (!isDSP) {
					log.warn(code + " already logged in from " + ac2.getRemoteAddr() + ", closing existing connection from " + remoteAddr);
					ac2.close();
					ctx.getACARSConnectionPool().remove(ac2);
					
					// Mark the connection closed
					SetConnection dao = new SetConnection(c);
					dao.closeConnection(ac2.getID());
				} else
					log.warn("Dispatcher " + code + " already logged in from " + ac2.getRemoteAddr() + ", logging in from " + remoteAddr);
			}
		} catch (SecurityException se) {
			log.warn("Authentication Failure for " + msg.getUserID());
			AcknowledgeMessage errMsg = new AcknowledgeMessage(null, msg.getID());
			if ((usr != null) && (usr.getACARSRestriction() == Restriction.BLOCK))
				errMsg.setEntry("error", "ACARS Server access disabled");
			else if (cInfo.isDispatch() && (usr != null) && !usr.isInRole("Dispatch"))
				errMsg.setEntry("error", "Dispatch not authorized");
			else if (!StringUtils.isEmpty(se.getMessage()))
				errMsg.setEntry("error", "Authentication Failed - " + se.getMessage());
			else
				errMsg.setEntry("error", "Authentication Failed");

			ctx.push(errMsg, env.getConnectionID());
			usr = null;
		} catch (ACARSException ae) {
			AcknowledgeMessage errMsg = new AcknowledgeMessage(null, msg.getID());
			errMsg.setEntry("error", ae.getMessage());
			log.warn(msg.getUserID() + " - " + ae.getMessage());	
			ctx.push(errMsg, env.getConnectionID());
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
		if ((usr == null) || (con == null) || (ud == null))
			return;

		// Calculate the difference in system times, assume 500ms latency - don't allow logins if over 4h off
		Instant now = Instant.now();
		long timeDiff = (msg.getClientUTC().toEpochMilli() - now.toEpochMilli() + 500) / 1000;
		if (Math.abs(timeDiff) > 14400) {
			log.error("Cannot authenticate " + usr.getName() + " system clock " + timeDiff + " seconds off");

			// Convert times to client date/time
			ZonedDateTime zdt = ZonedDateTime.ofInstant(msg.getClientUTC(), usr.getTZ().getZone());
			ZonedDateTime zdn = ZonedDateTime.ofInstant(now, usr.getTZ().getZone());

			AcknowledgeMessage errMsg = new AcknowledgeMessage(null, msg.getID());
			errMsg.setEntry("error", "It is now " + zdn.toString() + ". Your system clock is set to " + zdt.toString() + " ( " + timeDiff + " seconds off)");
			ctx.push(errMsg, env.getConnectionID());
			return;
		} else if (Math.abs(timeDiff) > 900)
			log.warn(usr.getName() + " system clock " + timeDiff + " seconds off");

		// Log the user in
		con.setUser(usr);
		con.setUserLocation(ud);
		con.setVersion(cInfo.getVersion());
		con.setClientBuild(cInfo.getClientBuild(), cInfo.getBeta());
		con.setUserHidden(msg.isHidden() && usr.isInRole("HR"));
		con.setTimeOffset(timeDiff * 1000);
		if (msg.getProtocolVersion() > con.getProtocolVersion()) {
			log.info(usr.getName() + " requesting protocol v" + msg.getProtocolVersion());
			con.setProtocolVersion(msg.getProtocolVersion());
		}
		
		// If we're a dispatcher, set the default location and range
		switch (cInfo.getClientType()) {
			case DISPATCH:
				con.setIsDispatch(true);
				con.setRange(SystemData.getAirport(usr.getHomeAirport()), Integer.MAX_VALUE);
				break;
				
			case ATC:
				con.setIsATC(true);
				break;
				
			default:
				break;
		}
		 
		// Save the connection data
		boolean requestSystemInfo = false; int heldFlights = 0;
		try {
			Connection c = ctx.getConnection();
			
			// Get the IP Address info
			GetIPLocation ipdao = new GetIPLocation(c);
			con.setAddressInfo(ipdao.get(con.getRemoteAddr()));
			
			// Load MVS warnings and disable voice if necessary
			if (SystemData.getBoolean("acars.voice.enabled")) {
				GetWarnings wdao = new GetWarnings(c);
				con.setWarnings(wdao.getCount(usr.getID()));
				if (con.getWarnings() >= SystemData.getInt("acars.voice.maxWarnings", 3)) {
					log.warn(usr.getName() + " has " + con.getWarnings() + " warnings, voice disabled");
					usr.setNoVoice(true);
				}
			}
			
			// Check if we need to request system data
			if (con.getProtocolVersion() > 1) {
				GetSystemInfo sysdao = new GetSystemInfo(c);
				SystemInformation sysinfo = sysdao.get(usr.getID());
				requestSystemInfo = (sysinfo == null) || ((now.toEpochMilli() - sysinfo.getDate().toEpochMilli()) > (86400_000 * 6));
			}
			
			// Check for held flights
			GetFlightReports frdao = new GetFlightReports(c);
			heldFlights = frdao.getHeld(ud.getID(), ud.getDB());

			// Start a transaction
			ctx.startTX();

			// Get the DAO and write the connection
			if (con.getIsDispatch()) {
				SetConnection cwdao = new SetConnection(c);
				cwdao.add(con);
			}
			
			// Log the login
			SetPilotLogin pwdao = new SetPilotLogin(c);
			pwdao.login(ud.getID(), con.getRemoteHost(), ud.getDB());

			// Save login hostname/IP address forever
			try {
				InetAddress addr = InetAddress.getByName(con.getRemoteAddr());
				if (!addr.isLinkLocalAddress() && !addr.isSiteLocalAddress()) {
					SetSystemData swdao = new SetSystemData(c);
					swdao.login(ud.getDB(), ud.getID(), con.getRemoteAddr(), con.getRemoteHost());
				}
			} catch (UnknownHostException uhe) {
				log.warn("Unknown Host " + con.getRemoteAddr());
			}

			// If Teamspeak is enabled, mark us as logged in
			if (SystemData.getBoolean("airline.voice.ts2.enabled")) {
				SetTS2Data ts2wdao = new SetTS2Data(c);
				ts2wdao.setActive(usr.getPilotCode(), true);
			}

			// Commit
			ctx.commitTX();

			// Add the user again so they are registered with this user ID
			ctx.getACARSConnectionPool().add(con);
		} catch (ACARSException ae) {
			log.error("Error logging connection - " + ae.getMessage());
		} catch (DAOException de) {
			ctx.rollbackTX();
			log.error("Error logging connection - " + de.getMessage(), de);
		} finally {
			ctx.release();
		}

		// Tell everybody else that someone has logged on
		ConnectionMessage drMsg = new ConnectionMessage(usr, DataRequest.ADDUSER, 0); // Set this to zero since the message ID is from another user
		drMsg.add(con);
		if (con.getUserHidden()) {
			for (ACARSConnection ac : ctx.getACARSConnectionPool().getAll()) {
				if ((ac.getID() != con.getID()) && ac.getUser().isInRole("HR"))
					ctx.push(drMsg, ac.getID());
			}
		} else
			ctx.pushAll(drMsg, con.getID());

		// If we have a newer ACARS client build, say so
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(usr, msg.getID());
		if (!con.getIsDispatch() && (latestClient != null) && (latestClient.getClientBuild() > cInfo.getClientBuild()))
			ackMsg.setEntry("latestBuild", String.valueOf(latestClient.getClientBuild()));
		
		// Set roles/ratings and if we are unrestricted
		ackMsg.setEntry("userID", usr.getPilotCode());
		ackMsg.setEntry("dbID", String.valueOf(usr.getID()));
		ackMsg.setEntry("rank", usr.getRank().getName());
		ackMsg.setEntry("timeOffset", String.valueOf(timeDiff / 1000));
		ackMsg.setEntry("roles", StringUtils.listConcat(usr.getRoles(), ","));
		ackMsg.setEntry("networks", StringUtils.listConcat(usr.getNetworks(), ","));
		ackMsg.setEntry("ratings", StringUtils.listConcat(usr.getRatings(), ","));
		ackMsg.setEntry("airportCode", usr.getAirportCodeType().toString());
		ackMsg.setEntry("distanceUnits", String.valueOf(usr.getDistanceType().ordinal()));
		ackMsg.setEntry("weightUnits", String.valueOf(usr.getWeightType().ordinal()));
		ackMsg.setEntry("systemInfo", String.valueOf(requestSystemInfo));
		ackMsg.setEntry("heldFlights", String.valueOf(heldFlights));
		if ((con.getCompression() == Compression.NONE) && (con.getProtocolVersion() > 1) && msg.getHasCompression())
			ackMsg.setEntry("compress", String.valueOf(SystemData.getBoolean("acars.compress")));
		if ((usr.getRoles().size() > 2) || (usr.getACARSRestriction() == Restriction.OK))
			ackMsg.setEntry("unrestricted", "true");
		else if (usr.getACARSRestriction() == Restriction.NOMSGS)
			ackMsg.setEntry("noMsgs", "true");
		
		// Check if airline uses SSL
		AirlineInformation ai = SystemData.getApp(ud.getAirlineCode());
		ackMsg.setEntry("useSSL", String.valueOf(ai.getSSL()));
		
		// Get max time acceleration rate
		if (!con.getIsDispatch()) {
			if (!usr.getNoTimeCompression()) {
				Map<?, ?> maxAccels = (Map<?, ?>) SystemData.getObject("acars.maxAccel");
				if (maxAccels != null) {
					String maxAccel = (String) maxAccels.get(ud.getAirlineCode().toLowerCase());
					ackMsg.setEntry("maxAccel", StringUtils.isEmpty(maxAccel) ? "4" : maxAccel);
				}
			} else
				ackMsg.setEntry("maxAccel", "1");
		}
		
		// Check if we can add voice channels
		if (SystemData.getBoolean("acars.voice.enabled")) {
			@SuppressWarnings("unchecked")
			Collection<String> newChannelRoles = (Collection<String>) SystemData.getObject("acars.voice.newChannelRoles");
			if (newChannelRoles != null)
				ackMsg.setEntry("tempChannel", String.valueOf(RoleUtils.hasAccess(usr.getRoles(), newChannelRoles)));
		}

		// Send the ack message
		ctx.push(ackMsg, env.getConnectionID(), true);

		// Return a system message to the user
		SystemTextMessage sysMsg = new SystemTextMessage();
		sysMsg.addMessage("Welcome to the " + SystemData.get("airline.name") + " ACARS server! (Build " + VersionInfo.BUILD + ")");
		sysMsg.addMessage(VersionInfo.TXT_COPYRIGHT);
		if (StringUtils.isEmpty(usr.getPilotCode()))
			sysMsg.addMessage("You are logged in as " + usr.getName() + " from " + con.getRemoteAddr());
		else
			sysMsg.addMessage("You are logged in as " + usr.getName() + " (" + usr.getPilotCode() + ") from " + con.getRemoteAddr());
			
		// Add system-defined messages
		@SuppressWarnings("unchecked")
		Collection<? extends String> systemMsgs = (Collection<? extends String>) SystemData.getObject("acars.login_msgs");
		if (systemMsgs != null)
			sysMsg.addMessages(systemMsgs);

		// Add hidden/dispatch notices
		if (con.getUserHidden())
			sysMsg.addMessage("You are in STEALTH mode and do not appear in the connections list.");
		if (con.getIsDispatch())
			sysMsg.addMessage("You are currently operating as a Dispatcher.");
		else if (con.getIsATC())
			sysMsg.addMessage("You are currently operating as an Air Traffic Controller.");

		// Send the message and log
		ctx.push(sysMsg, env.getConnectionID());
		log.info("New Connection from " + usr.getName() + " (" + con.getVersion() + ")");
	}

	/**
	 * Returns the maximum execution time of this command before a warning is issued.
	 * @return the maximum execution time in milliseconds
	 */
	@Override
	public final int getMaxExecTime() {
		return 2250;
	}
}