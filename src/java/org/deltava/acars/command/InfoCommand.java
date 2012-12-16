// Copyright 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;
import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import org.deltava.beans.*;
import org.deltava.beans.acars.*;
import org.deltava.beans.flight.*;
import org.deltava.beans.navdata.TerminalRoute;
import org.deltava.beans.schedule.FlightTime;
import org.deltava.beans.schedule.ScheduleRoute;
import org.deltava.beans.testing.*;

import org.deltava.dao.*;
import org.deltava.dao.acars.SetInfo;

import org.deltava.util.StringUtils;

/**
 * An ACARS Command to log Flight data.
 * @author Luke
 * @version 5.1
 * @since 1.0
 */

public class InfoCommand extends ACARSCommand {

	private static final Logger log = Logger.getLogger(InfoCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the message
		InfoMessage msg = (InfoMessage) env.getMessage();
		String flightType = msg.isOffline() ? "Offline" : "Online";

		// Check if we already have a flight ID and are requesting a new one
		boolean assignID = (msg.getFlightID() == 0);
		ACARSConnection con = ctx.getACARSConnection();
		InfoMessage curInfo = con.getFlightInfo();
		UserData usrLoc = con.getUserData();
		if (con.getIsDispatch()) {
			log.warn("Dispatch Client requesting flight ID!");
			return;
		}

		// Build the acknowledge message
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());

		// Check for a duplicate Flight ID request
		if (assignID && (curInfo != null) && (curInfo.getFlightID() != 0) && (!curInfo.isComplete())) {
			msg.setFlightID(curInfo.getFlightID());
			log.warn("Duplicate Flight ID request - assigning Flight ID " + msg.getFlightID());

			// Send back the acknowledgement
			ackMsg.setEntry("flight_id", String.valueOf(msg.getFlightID()));
			ctx.push(ackMsg, env.getConnectionID());
			return;
		}

		// Write the info to the database
		int oldID = msg.getFlightID();
		try {
			Connection c = ctx.getConnection();

			// If we're requesting a specific ID, make sure we used to own it
			boolean isValidated = false;
			if (!assignID) {
				GetACARSData rdao = new GetACARSData(c);
				FlightInfo info = rdao.getInfo(msg.getFlightID());
				if (info == null) {
					log.warn(env.getOwnerID() + " requesting invalid Flight " + msg.getFlightID());
					assignID = true;
					msg.setFlightID(0);
				} else if (info.getAuthorID() != env.getOwner().getID()) {
					log.warn(env.getOwnerID() + " requesting owned Flight " + msg.getFlightID());
					assignID = true;
					msg.setFlightID(0);
				} else if (info.getArchived() || info.getHasPIREP()) {
					log.warn(msg.getFlightID() + " has PIREP or is archived!");
					assignID = true;
					msg.setFlightID(0);
				} else
					isValidated = info.isScheduleValidated();
			}
			
			// Validate against the schedule - do this even if the message claims it's valid
			if (!isValidated) {
				ScheduleRoute rt = new ScheduleRoute(msg.getAirportD(), msg.getAirportA());
				GetSchedule sdao = new GetSchedule(c);
				FlightTime avgTime = sdao.getFlightTime(rt, usrLoc.getDB());
				msg.setScheduleValidated(avgTime.getFlightTime() > 0);
				
				// If we're not valid, check against draft PIREPs
				if (!msg.isScheduleValidated()) {
					GetFlightReports prdao = new GetFlightReports(c);
					Collection<FlightReport> pireps = prdao.getDraftReports(usrLoc.getID(), msg.getAirportD(), msg.getAirportA(), usrLoc.getDB());
					for (Iterator<FlightReport> i = pireps.iterator(); i.hasNext() && !msg.isScheduleValidated(); ) {
						FlightReport fr = i.next();
						boolean isOK = fr.hasAttribute(FlightReport.ATTR_CHARTER) || (fr.getDatabaseID(DatabaseID.ASSIGN) > 0);
						isOK &= msg.getAirportD().equals(fr.getAirportD());
						isOK &= msg.getAirportA().equals(fr.getAirportA());
						msg.setScheduleValidated(isOK);
					}
				}
				
				// If we're still not valid, check for an event
				if (!msg.isScheduleValidated()) {
					GetEvent edao = new GetEvent(c); 
					boolean isOK = (edao.getEvent(msg.getAirportD(), msg.getAirportA(), OnlineNetwork.VATSIM) > 0);
					isOK |= (edao.getEvent(msg.getAirportD(), msg.getAirportA(), OnlineNetwork.VATSIM) > 0);
					msg.setScheduleValidated(isOK);
				}
			} else
				msg.setScheduleValidated(true);

			// Look for a checkride record
			GetExam exdao = new GetExam(c);
			CheckRide cr = exdao.getCheckRide(usrLoc.getID(), msg.getEquipmentType(), TestStatus.NEW);
			msg.setCheckRide(cr != null);
			ackMsg.setEntry("checkRide", String.valueOf(msg.isCheckRide()));
			if (cr != null)
				ackMsg.setEntry("crName", cr.getName());
			
			// Get the SID/STAR data
			GetNavAirway navdao = new GetNavAirway(c);
			TerminalRoute sid = navdao.getRoute(msg.getAirportD(), TerminalRoute.Type.SID, msg.getSID(), true);
			TerminalRoute star = navdao.getRoute(msg.getAirportA(), TerminalRoute.Type.STAR, msg.getSTAR(), true);
			
			// Log unknown SID/STAR
			if ((sid == null) && (!StringUtils.isEmpty(msg.getSID())))
				log.warn("Unknown SID - " + msg.getSID());
			if ((star == null) && (!StringUtils.isEmpty(msg.getSTAR())))
				log.warn("Unknown STAR - " + msg.getSTAR());
			
			// Validate the dispatch route
			if (msg.getRouteID() != 0) {
				GetACARSRoute ardao = new GetACARSRoute(c);
				DispatchRoute rt = ardao.getRoute(msg.getRouteID());
				if (rt == null) {
					log.warn("Invalid Dispatch Route ID - " + msg.getRouteID());
					msg.setRouteID(0);
				}
			}
			
			// Validate the dispatcher
			if (msg.getDispatcherID() != 0) {
				GetUserData uddao = new GetUserData(c);
				UserData ud = uddao.get(msg.getDispatcherID());
				if (ud == null) {
					log.warn("Invalid Dispatcher ID - " + msg.getDispatcherID());
					msg.setDispatcherID(0);
				}
			}
			
			// Start a transaction
			ctx.startTX();
			
			// Write the flight information
			SetInfo iwdao = new SetInfo(c);
			iwdao.write(con, msg);
			
			// Write the SID/STAR data
			SetACARSData dwdao = new SetACARSData(c);
			if (sid != null) {
				dwdao.clearSID(msg.getFlightID());
				dwdao.writeSIDSTAR(msg.getFlightID(), sid);
			}
			if (star != null) {
				dwdao.clearSTAR(msg.getFlightID());
				dwdao.writeSIDSTAR(msg.getFlightID(), star);
			}
			
			// Commit the transaction
			ctx.commitTX();
		} catch (DAOException de) {
			ctx.rollbackTX();
			msg.setFlightID(oldID);
			log.error("Pilot = " + con.getUserID() + ", RouteID = " + msg.getRouteID() + ", DispatcherID = " + msg.getDispatcherID());
			log.error(de.getMessage(), de);
		} finally {
			ctx.release();
		}

		// Log returned flight id
		if (assignID)
			log.info("Assigned " + flightType + " Flight ID " + String.valueOf(msg.getFlightID()) + " to " + env.getOwnerID());
		else
			log.info(env.getOwnerID() + " resuming Flight " + msg.getFlightID());

		// Create the ack message and envelope - these are always acknowledged
		ackMsg.setEntry("flight_id", String.valueOf(msg.getFlightID()));
		ackMsg.setEntry("schedValid", String.valueOf(msg.isScheduleValidated()));
		ctx.push(ackMsg, env.getConnectionID(), true);

		// Set the info for the connection and write it to the database
		con.setFlightInfo(msg);
		log.info("Received " + flightType + " flight information from " + con.getUserID());
	}
}