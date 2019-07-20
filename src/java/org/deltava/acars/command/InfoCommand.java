// Copyright 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2014, 2016, 2017, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;
import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;
import org.deltava.acars.util.SquawkGenerator;

import org.deltava.beans.*;
import org.deltava.beans.acars.*;
import org.deltava.beans.flight.*;
import org.deltava.beans.navdata.TerminalRoute;
import org.deltava.beans.schedule.*;
import org.deltava.beans.testing.*;

import org.deltava.dao.*;
import org.deltava.dao.acars.*;
import org.deltava.util.FlightCodeParser;
import org.deltava.util.StringUtils;

/**
 * An ACARS Command to log Flight data.
 * @author Luke
 * @version 8.6
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

		// Check if we already have a flight ID and are requesting a new one
		boolean assignID = (msg.getFlightID() == 0);
		ACARSConnection con = ctx.getACARSConnection();
		InfoMessage curInfo = con.getFlightInfo();
		UserData usrLoc = con.getUserData();
		if (con.getIsDispatch()) {
			log.warn("Dispatch Client requesting flight ID!");
			return;
		} else if (!con.getUserID().equals(env.getOwnerID()))
			log.warn("Connection owned by " + con.getUserID() + " Envelope owned by " + env.getOwnerID());

		// Build the acknowledge message
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());

		// Check for a duplicate Flight ID request
		if (assignID && (curInfo != null) && (curInfo.getFlightID() != 0) && !curInfo.isComplete()) {
			log.warn("Duplicate Flight ID request - assigning Flight ID " + curInfo.getFlightID());
			ackMsg.setEntry("flight_id", String.valueOf(curInfo.getFlightID()));
			ackMsg.setEntry("tx", String.valueOf(curInfo.getTX()));
			ctx.push(ackMsg);
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
					log.warn(env.getOwnerID() + " requesting Flight " + msg.getFlightID() + " - owned by ID " + info.getAuthorID());
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
				msg.setScheduleValidated(avgTime.getType() != RoutePairType.UNKNOWN);
				
				// If we're not valid, check against draft PIREPs
				if (!msg.isScheduleValidated()) {
					GetFlightReports prdao = new GetFlightReports(c);
					Collection<FlightReport> pireps = prdao.getDraftReports(usrLoc.getID(), msg, usrLoc.getDB());
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
					boolean isOK = (edao.getPossibleEvent(msg, OnlineNetwork.VATSIM, msg.getStartTime()) > 0);
					isOK |= (edao.getPossibleEvent(msg, OnlineNetwork.IVAO, msg.getStartTime()) > 0);
					msg.setScheduleValidated(isOK);
				}
			} else
				msg.setScheduleValidated(true);
			
			// Look for a check ride record - Builds prior to 103 send no check ride flag, but submit on PIREP
			boolean properCRHandling = (con.getClientBuild() > 103);
			if (!msg.isNoRideCheck()) {
				GetExam exdao = new GetExam(c);
				CheckRide cr = exdao.getCheckRide(usrLoc.getID(), msg.getEquipmentType(), TestStatus.NEW);
				ackMsg.setEntry("checkRide", String.valueOf(cr != null));
				if (!properCRHandling)
					msg.setCheckRide(cr != null);
			} else if (properCRHandling && msg.isCheckRide())
				ackMsg.setEntry("checkRide", "true");
			
			// Check for ontime
			if (msg.isScheduleValidated() && (msg.getSimStartTime() != null) && !msg.isCheckRide()) {
				Flight fe = FlightCodeParser.parse(msg.getFlightCode(), usrLoc.getAirlineCode());
				GetScheduleSearch sdao = new GetScheduleSearch(c);
				ScheduleSearchCriteria ssc = new ScheduleSearchCriteria("TIME_D"); ssc.setDBName(usrLoc.getDB());
				ssc.setAirportD(msg.getAirportD()); ssc.setAirportA(msg.getAirportA());
				ssc.setExcludeHistoric((fe == null) || !fe.getAirline().getHistoric() ? Inclusion.EXCLUDE : Inclusion.INCLUDE);
				OnTimeHelper oth = new OnTimeHelper(sdao.search(ssc));
				ackMsg.setEntry("onTime", String.valueOf(oth.validateDeparture(msg)));
				if (oth.getScheduleEntry() != null) {
					ScheduleEntry se = oth.getScheduleEntry();
					ackMsg.setEntry("onTimeFlight", se.getFlightCode());
					ackMsg.setEntry("onTimeLeg", String.valueOf(se.getLeg()));
					ackMsg.setEntry("onTimeDeparture", StringUtils.format(se.getTimeD(), "HH:mm"));
					ackMsg.setEntry("onTimeArrival", StringUtils.format(se.getTimeA(), "HH:mm"));
				}
			}
			
			// Load passenger count for 121+ that submits load factor
			GetAircraft acdao = new GetAircraft(c);
			Aircraft ac = acdao.get(msg.getEquipmentType());
			if (ac != null) {
				if (msg.getPassengers() > 0)
					msg.setLoadFactor(msg.getPassengers() * 1.0d / ac.getSeats());
				else if ((msg.getLoadFactor() > 0) && (msg.getPassengers() == 0))
					msg.setPassengers((int) (ac.getSeats() * msg.getLoadFactor()));
			} else
				log.warn("Unknown aircraft type - " + msg.getEquipmentType());
				
			// Get the SID/STAR data
			GetNavAirway navdao = new GetNavAirway(c);
			TerminalRoute sid = navdao.getRoute(msg.getAirportD(), TerminalRoute.Type.SID, msg.getSID(), true);
			TerminalRoute star = navdao.getRoute(msg.getAirportA(), TerminalRoute.Type.STAR, msg.getSTAR(), true);
			
			// Log unknown SID/STAR
			if ((sid == null) && (!StringUtils.isEmpty(msg.getSID())))
				log.warn("Unknown SID - " + msg.getSID());
			else if ((sid != null) && !sid.getCode().equals(msg.getSID()))
				msg.setSID(sid.getCode());
			if ((star == null) && (!StringUtils.isEmpty(msg.getSTAR())))
				log.warn("Unknown STAR - " + msg.getSTAR());
			else if ((star != null) && !star.getCode().equals(msg.getSTAR()))
				msg.setSTAR(star.getCode());
			
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
			
			// Generate a squawk code
			if ((msg.getNetwork() == null) && TXCode.isDefault(msg.getTX())) {
				GetTXCodes txdao = new GetTXCodes(c);
				Map<Integer, TXCode> codes = txdao.getCodes();
				int genCount = 0;
				while (TXCode.isDefault(msg.getTX()) && (genCount < 20)) {
					genCount++;
					TXCode tx = SquawkGenerator.generate((genCount < 10) ? msg.getAirportD() : null);
					TXCode tx2 = codes.get(Integer.valueOf(tx.getCode()));
					if ((tx2 == null) || (tx2.getID() == usrLoc.getID()))
						msg.setTX(tx.getCode());
					else
						log.warn("Squawk code " + tx + " already assigned");
				}
			}
			
			// Write the flight information
			SetInfo iwdao = new SetInfo(c);
			iwdao.write(con, msg);
			
			// Write the SID/STAR data
			SetACARSData dwdao = new SetACARSData(c);
			if (sid != null) {
				dwdao.clearTerminalRoutes(msg.getFlightID(), TerminalRoute.Type.SID);
				dwdao.writeSIDSTAR(msg.getFlightID(), sid);
			}
			if (star != null) {
				dwdao.clearTerminalRoutes(msg.getFlightID(), TerminalRoute.Type.STAR);
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
			log.info("Assigned Flight ID " + String.valueOf(msg.getFlightID()) + " to " + env.getOwnerID());
		else
			log.info(env.getOwnerID() + " resuming Flight " + msg.getFlightID());

		// Create the ack message and envelope - these are always acknowledged
		ackMsg.setEntry("flight_id", String.valueOf(msg.getFlightID()));
		ackMsg.setEntry("tx", String.valueOf(msg.getTX()));		
		ackMsg.setEntry("schedValid", String.valueOf(msg.isScheduleValidated()));
		ctx.push(ackMsg, env.getConnectionID(), true);

		// Set the info for the connection
		con.setFlightInfo(msg);
		log.info("Received flight information from " + con.getUserID());
	}
}