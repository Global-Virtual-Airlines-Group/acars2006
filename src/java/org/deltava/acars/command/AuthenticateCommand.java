// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2018, 2019, 2020, 2022, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.net.*;
import java.sql.*;
import java.util.*;
import java.time.*;

import org.apache.logging.log4j.*;

import org.deltava.beans.*;
import org.deltava.beans.acars.*;
import org.deltava.beans.econ.*;
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

import org.gvagroup.common.SharedData;

/**
 * An ACARS server command to authenticate a user.
 * @author Luke
 * @version 11.1
 * @since 1.0
 */

public class AuthenticateCommand extends ACARSCommand {

	private static final Logger log = LogManager.getLogger(AuthenticateCommand.class);

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
			ctx.push(errMsg);
			return;
		}

		// Get the user ID and check for valid airline code
		UserID usrID = new UserID(msg.getUserID());
		AirlineInformation aInfo = SystemData.getApp(usrID.getAirlineCode());
		if (aInfo == null)
			aInfo = SystemData.getApp(SystemData.get("airline.default"));

		ClientInfo cInfo = msg.getClientInfo(); boolean isDispatch = (cInfo.getClientType() == ClientType.DISPATCH);
		ClientInfo latestClient = null; UserData ud = null; Pilot usr = null;
		try {
			Connection c = ctx.getConnection();
			
			// Get the minimum build number
			GetACARSBuilds abdao = new GetACARSBuilds(c);
			latestClient = abdao.getLatestBuild(cInfo);
			boolean isOK = abdao.isValid(cInfo);
			if (!isOK || (latestClient == null)) {
				String ct = cInfo.getClientType().getDescription();
				if (latestClient == null)
					throw new ACARSException(String.format("Unknown/Deprecated ACARS %s Version - %d", ct, Integer.valueOf(cInfo.getVersion())));	
				else if (cInfo.isBeta())
					throw new ACARSException(String.format("Unknown/Deprecated ACARS beta - Build %d Beta %d", Integer.valueOf(cInfo.getClientBuild()), Integer.valueOf(cInfo.getBeta())));	
				
				throw new ACARSException(String.format("Obsolete ACARS %s Client - Use Build %d or newer", ct, Integer.valueOf(latestClient.getClientBuild())));	
			}

			// Get the DAOs
			GetPilot pdao = new GetPilot(c);
			GetUserData udao = new GetUserData(c);

			// If we're using a database ID to authenticate, load it differently
			if (!usrID.hasAirlineCode()) {
				ud = udao.get(StringUtils.parse(msg.getUserID(), 0));
				if (ud == null)
					throw new SecurityException(String.format("Invalid database ID - %s", msg.getUserID()));

				usr = pdao.get(ud);
			} else {
				usr = pdao.getPilotByCode(usrID.getUserID(), aInfo.getDB());
				if (usr != null)
					ud = udao.get(usr.getID());
			}

			// Check security access before we validate the password
			if ((usr == null) || (usr.getStatus() != PilotStatus.ACTIVE) || (usr.getACARSRestriction() == Restriction.BLOCK))
				throw new SecurityException();
			else if (isDispatch && (!usr.isInRole("Dispatch")))
				throw new SecurityException("Invalid dispatch access");
			else if (ud == null)
				throw new SecurityException(String.format("Cannot load user data - %s", msg.getUserID()));
			
			// Validate the password
			try (org.deltava.security.Authenticator auth = (org.deltava.security.Authenticator) SystemData.getObject(SystemData.AUTHENTICATOR)) {
				if (auth instanceof SQLAuthenticator sa) sa.setConnection(c); 
				auth.authenticate(usr, msg.getPassword());
			}
			
			// Check if we're already logged in
			ACARSConnection ac2 = ctx.getACARSConnection(usr.getPilotCode());
			if (ac2 != null) {
				String code = StringUtils.isEmpty(usr.getPilotCode()) ? usr.getName() : usr.getPilotCode();
				String remoteAddr = ctx.getACARSConnection().getRemoteAddr();
				boolean isDSP = (isDispatch || ac2.getIsDispatch());
				if (!isDSP) {
					log.warn("{} ({}) already logged in from {}, closing existing connection from {}", usr.getName(), code, ac2.getRemoteAddr(), remoteAddr);
					ac2.close();
					ctx.getACARSConnectionPool().remove(ac2);
					
					// Mark the connection closed
					SetConnection dao = new SetConnection(c);
					dao.closeConnection(ac2.getID());
				} else
					log.warn("Dispatcher {} ({}) already logged in from {}, logging in from {}", usr.getName(), code, ac2.getRemoteAddr(), remoteAddr);
			}
		} catch (SecurityException se) {
			log.warn("Authentication Failure for " + msg.getUserID());
			AcknowledgeMessage errMsg = new AcknowledgeMessage(null, msg.getID());
			if ((usr != null) && (usr.getACARSRestriction() == Restriction.BLOCK))
				errMsg.setEntry("error", "ACARS Server access disabled");
			else if (isDispatch && (usr != null) && !usr.isInRole("Dispatch"))
				errMsg.setEntry("error", "Dispatch not authorized");
			else if (!StringUtils.isEmpty(se.getMessage()))
				errMsg.setEntry("error", String.format("Authentication Failed - %s", se.getMessage()));
			else
				errMsg.setEntry("error", "Authentication Failed");

			ctx.push(errMsg);
			usr = null;
		} catch (ACARSException ae) {
			log.warn("{} - {}", msg.getUserID(), ae.getMessage());	
			ctx.push(new AcknowledgeMessage(null, msg.getID(), ae.getMessage()));
		} catch (DAOException de) {
			usr = null;
			String errorMsg = String.format("Error loading %s - %s", msg.getUserID(), de.getMessage());
			if (de.isWarning())
				log.warn(errorMsg);
			else
				log.error(errorMsg, de.getLogStackDump() ? de : null);

			ctx.push(new AcknowledgeMessage(null, msg.getID(), String.format("Authentication Failed - %s", de.getMessage())));
		} catch (Exception e) {
			usr = null;
			log.atError().withThrowable(e).log("Error loading {} - {}", msg.getUserID(), e.getMessage());
			ctx.push(new AcknowledgeMessage(null, msg.getID(), String.format("Authentication Failed - %s", e.getMessage())));
		} finally {
			ctx.release();
		}

		// Get the ACARS connection
		ACARSConnection con = ctx.getACARSConnection();
		if ((usr == null) || (con == null) || (ud == null))
			return;

		// Calculate the difference in system times, assume 500ms latency - don't allow logins if over 4h off
		Duration td = Duration.between(msg.getClientUTC(), Instant.now().plusMillis(500));
		if (td.abs().toSeconds() > 14400) {
			log.warn("Cannot authenticate {} - system clock {} seconds off", usr.getName(), Long.valueOf(td.toSeconds()));

			// Convert times to client date/time
			ZonedDateTime zdt = ZonedDateTime.ofInstant(msg.getClientUTC(), usr.getTZ().getZone());
			ZonedDateTime zdn = ZonedDateTime.ofInstant(Instant.now(), usr.getTZ().getZone());
			ctx.push(new AcknowledgeMessage(null, msg.getID(), String.format("It is now %s. Your system clock is set to %s (%d seconds off)", zdn, zdt, Long.valueOf(td.toSeconds()))));
			return;
		} else if (td.abs().toSeconds() > 900)
			log.warn("{} system clock {} seconds off", usr.getName(), Long.valueOf(td.toSeconds()));

		// Log the user in
		con.setUser(usr);
		con.setUserLocation(ud);
		con.setVersion(cInfo.getVersion());
		con.setClientBuild(cInfo.getClientBuild(), cInfo.getBeta());
		con.setUserHidden(msg.isHidden() && usr.isInRole("HR"));
		con.setTimeOffset(td.toSeconds());
		if (msg.getProtocolVersion() > con.getProtocolVersion()) {
			log.info(String.format("%s requesting protocol v%d", usr.getName(), Integer.valueOf(msg.getProtocolVersion())));
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
				con.setWarningScore(wdao.get(usr.getID()).stream().mapToInt(Warning::getScore).sum());
				if (con.getWarningScore() >= SystemData.getInt("acars.maxWarnings", 10)) {
					log.warn(String.format("%s has %d warning score, voice/text disabled", usr.getName(), Integer.valueOf(con.getWarningScore())));
					usr.setNoVoice(true);
					usr.setACARSRestriction(Restriction.NOMSGS);
				}
			}
			
			// Check if we need to request system data
			if (con.getProtocolVersion() > 1) {
				GetSystemInfo sysdao = new GetSystemInfo(c);
				SystemInformation sysinfo = sysdao.get(usr.getID());
				if (sysinfo != null) {
					Duration id = Duration.between(sysinfo.getDate(), Instant.now());
					requestSystemInfo = (id.toDays() > 6);
				} else
					requestSystemInfo = true;
			}
			
			// Check for held flights
			GetFlightReports frdao = new GetFlightReports(c);
			heldFlights = frdao.getHeld(ud.getID(), ud.getDB());
			
			// Get elite status
			Object epo = SharedData.get(SharedData.ELITE_INFO + aInfo.getCode());
			if (epo != null) {
				GetElite eldao = new GetElite(c);
				Map<Integer, EliteStatus> status = eldao.getStatus(List.of(ud), EliteScorer.getStatusYear(Instant.now()), ud.getDB());
				if (!status.isEmpty())
					con.setEliteStatus(status.get(ud.cacheKey()));
			}

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
				log.warn("Unknown Host - {}", con.getRemoteAddr());
			}

			ctx.commitTX();

			// Add the user again so they are registered with this user ID
			ctx.getACARSConnectionPool().add(con);
		} catch (ACARSException ae) {
			log.error("Error logging connection - {}", ae.getMessage());
		} catch (DAOException de) {
			ctx.rollbackTX();
			log.atError().withThrowable(de).log("Error logging connection - {}", de.getMessage());
		} finally {
			ctx.release();
		}

		// Tell everybody else that someone has logged on
		ConnectionMessage drMsg = new ConnectionMessage(usr, DataRequest.ADDUSER, 0); // Set this to zero since the message ID is from another user
		drMsg.add(con);
		if (con.getUserHidden()) {
			for (ACARSConnection ac : ctx.getACARSConnectionPool().getAll()) {
				if ((ac.getID() != con.getID()) && ac.isAuthenticated() && ac.getUser().isInRole("HR"))
					ctx.push(drMsg, ac.getID(), false);
			}
		} else
			ctx.pushAll(drMsg, env.getConnectionID());

		// If we have a newer ACARS client build, say so
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(usr, msg.getID());
		if (!con.getIsDispatch() && (latestClient != null) && (latestClient.getClientBuild() > cInfo.getClientBuild()))
			ackMsg.setEntry("latestBuild", String.valueOf(latestClient.getClientBuild()));
		
		// Set roles/ratings and if we are unrestricted
		ackMsg.setEntry("userID", usr.getPilotCode());
		ackMsg.setEntry("dbID", String.valueOf(usr.getID()));
		ackMsg.setEntry("appCode", ud.getAirlineCode());
		ackMsg.setEntry("rank", usr.getRank().getName());
		ackMsg.setEntry("timeOffset", String.valueOf(td.toSeconds()));
		ackMsg.setEntry("roles", StringUtils.listConcat(usr.getRoles(), ","));
		ackMsg.setEntry("networks", StringUtils.listConcat(usr.getNetworks(), ","));
		ackMsg.setEntry("ratings", StringUtils.listConcat(usr.getRatings(), ","));
		ackMsg.setEntry("airportCode", usr.getAirportCodeType().toString());
		ackMsg.setEntry("distanceUnits", String.valueOf(usr.getDistanceType().ordinal()));
		ackMsg.setEntry("weightUnits", String.valueOf(usr.getWeightType().ordinal()));
		ackMsg.setEntry("systemInfo", String.valueOf(requestSystemInfo));
		ackMsg.setEntry("heldFlights", String.valueOf(heldFlights));
		ackMsg.setEntry("updChannel", usr.getACARSUpdateChannel().name().toLowerCase());
		if ((con.getCompression() == Compression.NONE) && (con.getProtocolVersion() > 1) && msg.getHasCompression())
			ackMsg.setEntry("compress", String.valueOf(SystemData.getBoolean("acars.compress")));
		if ((usr.getRoles().size() > 2) || (usr.getACARSRestriction() == Restriction.OK))
			ackMsg.setEntry("unrestricted", "true");
		else if (usr.getACARSRestriction() == Restriction.NOMSGS)
			ackMsg.setEntry("noMsgs", "true");
		if (con.getEliteStatus() != null) {
			EliteLevel lvl = con.getEliteStatus().getLevel();
			ackMsg.setEntry("eliteLevel", lvl.getName());
			ackMsg.setEntry("eliteYear", String.valueOf(lvl.getYear()));
			ackMsg.setEntry("eliteColor", String.valueOf(lvl.getColor()));
		}
		
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
		
		// Send the ack message
		ctx.push(ackMsg, env.getConnectionID(), true);

		// Return a system message to the user
		SystemTextMessage sysMsg = new SystemTextMessage();
		sysMsg.addMessage(String.format("Welcome to the %s ACARS server! (Build %s)", aInfo.getName(), VersionInfo.getFullBuild()));
		sysMsg.addMessage(VersionInfo.TXT_COPYRIGHT);
		if (StringUtils.isEmpty(usr.getPilotCode()))
			sysMsg.addMessage(String.format("You are logged in as %s from %s", usr.getName(), con.getRemoteAddr()));
		else
			sysMsg.addMessage(String.format("You are logged in as %s (%s) from %s", usr.getName(), usr.getPilotCode(), con.getRemoteAddr()));
			
		// Add system-defined messages
		@SuppressWarnings("unchecked")
		Collection<? extends String> systemMsgs = (Collection<? extends String>) SystemData.getObject("acars.login_msgs");
		if (systemMsgs != null)
			systemMsgs.forEach(sysMsg::addMessage);

		// Add hidden/dispatch notices
		if (con.getUserHidden())
			sysMsg.addMessage("You are in STEALTH mode and do not appear in the connections list.");
		if (con.getIsDispatch())
			sysMsg.addMessage("You are currently operating as a Dispatcher.");
		else if (con.getIsATC())
			sysMsg.addMessage("You are currently operating as an Air Traffic Controller.");

		// Send the message and log
		ctx.push(sysMsg);
		log.info(String.format("New Connection from %s (v%d)", usr.getName(), Integer.valueOf(con.getVersion())));
	}

	@Override
	public final int getMaxExecTime() {
		return 1750;
	}
}