// Copyright 2004, 2005, 2006, 2007 Global Virtual Airline Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;
import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.beans.*;
import org.deltava.beans.testing.*;
import org.deltava.beans.event.Event;
import org.deltava.beans.schedule.*;
import org.deltava.beans.system.UserData;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import org.deltava.dao.*;
import org.deltava.dao.acars.*;

import org.deltava.util.*;
import org.deltava.util.system.SystemData;

/**
 * An ACARS command to file a Flight Report.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class FilePIREPCommand extends ACARSCommand {

	private static final Logger log = Logger.getLogger(FilePIREPCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Log PIREP filing
		log.info("Receiving PIREP from " + env.getOwner().getName() + " (" + env.getOwnerID() + ")");

		// Get the Message and the ACARS connection
		FlightReportMessage msg = (FlightReportMessage) env.getMessage();
		ACARSConnection ac = ctx.getACARSConnection();
		if ((ac == null) || ac.getIsDispatch())
			return;
		
		// Generate the response message
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(ac.getUser(), msg.getID());
		
		// Get the PIREP data and flight information
		ACARSFlightReport afr = msg.getPIREP();
		InfoMessage info = ac.getFlightInfo();
		UserData usrLoc = ac.getUserData();
		
		// Adjust the times
		afr.setStartTime(CalendarUtils.adjustMS(afr.getStartTime(), ac.getTimeOffset()));
		afr.setTaxiTime(CalendarUtils.adjustMS(afr.getTaxiTime(), ac.getTimeOffset()));
		afr.setTakeoffTime(CalendarUtils.adjustMS(afr.getTakeoffTime(), ac.getTimeOffset()));
		afr.setLandingTime(CalendarUtils.adjustMS(afr.getLandingTime(), ac.getTimeOffset()));
		afr.setEndTime(CalendarUtils.adjustMS(afr.getEndTime(), ac.getTimeOffset()));
		
		// If we have no flight info, then push it back
		if (info == null) {
			log.warn("No Flight Information for Connection " + StringUtils.formatHex(ac.getID()));
			ackMsg.setEntry("sendInfo", "true");
			ctx.push(ackMsg, env.getConnectionID());
			return;
		}
		
		Connection con = null;
		try {
			con = ctx.getConnection();
			GetFlightReports prdao = new GetFlightReports(con);
			
			// Check for existing PIREP with this flight ID
			ctx.setMessage("Checking for duplicate Flight Report from " + ac.getUserID());
			if ((info != null) && (info.getFlightID() != 0)) {
			   ACARSFlightReport afr2 = prdao.getACARS(usrLoc.getDB(), info.getFlightID());
			   if (afr2 != null) {
			      ctx.release();
			      
			      // Log warning and return an ACK
			      log.warn("Ignoring duplicate PIREP submission from " + ac.getUserID());
			      ctx.push(ackMsg, ac.getID());
			      return;
			   }
			}

			// If we found a draft flight report, save its database ID and copy its ID to the PIREP we will file
			ctx.setMessage("Checking for draft Flight Reports by " + ac.getUserID());
			List<FlightReport> dFlights = prdao.getDraftReports(usrLoc.getID(), afr.getAirportD(), afr.getAirportA(), usrLoc.getDB());
			if (!dFlights.isEmpty()) {
				FlightReport fr = dFlights.get(0);
				afr.setID(fr.getID());
				afr.setDatabaseID(FlightReport.DBID_ASSIGN, fr.getDatabaseID(FlightReport.DBID_ASSIGN));
				afr.setDatabaseID(FlightReport.DBID_EVENT, fr.getDatabaseID(FlightReport.DBID_EVENT));
			}
			
			// Check if it's an Online Event flight
			if ((afr.getDatabaseID(FlightReport.DBID_EVENT) == 0) && (afr.hasAttribute(FlightReport.ATTR_ONLINE_MASK))) {
				int networkID = Event.NET_VATSIM;
				if (afr.hasAttribute(FlightReport.ATTR_IVAO))
					networkID = Event.NET_IVAO;
				else if (afr.hasAttribute(FlightReport.ATTR_INTVAS))
					networkID = Event.NET_INTVAS;
				
				// Load the event ID
				GetEvent evdao = new GetEvent(con);
				afr.setDatabaseID(FlightReport.DBID_EVENT, evdao.getEvent(afr.getAirportD(), afr.getAirportA(), networkID));
			}
			
			// Reload the User
			GetPilot pdao = new GetPilot(con);
			GetPilot.invalidateID(usrLoc.getID());
			Pilot p = pdao.get(usrLoc);
			
			// Convert the date into the user's local time zone
			DateTime dt = new DateTime(afr.getDate());
			dt.convertTo(p.getTZ());
			afr.setDate(dt.getDate());
			
			// Check if this Flight Report counts for promotion
			ctx.setMessage("Checking type ratings for " + ac.getUserID());
			GetEquipmentType eqdao = new GetEquipmentType(con);
			Collection<String> promoEQ = eqdao.getPrimaryTypes(usrLoc.getDB(), afr.getEquipmentType());
			if (promoEQ.contains(p.getEquipmentType()))
				afr.setCaptEQType(promoEQ);
			
			// Check if the user is rated to fly the aircraft
			EquipmentType eq = eqdao.get(p.getEquipmentType(), usrLoc.getDB());
			if (!p.getRatings().contains(afr.getEquipmentType()) && !eq.getRatings().contains(afr.getEquipmentType()))
				afr.setAttribute(FlightReport.ATTR_NOTRATED, !afr.hasAttribute(FlightReport.ATTR_CHECKRIDE));
			
			// Check for historic aircraft
			GetAircraft acdao = new GetAircraft(con);
			Aircraft a = acdao.get(afr.getEquipmentType());
			afr.setAttribute(FlightReport.ATTR_HISTORIC, (a != null) && (a.getHistoric()));
			
			// Check for excessive distance
			if ((a != null) && (afr.getDistance() > a.getRange()))
				afr.setAttribute(FlightReport.ATTR_RANGEWARN, true);
			
			// Check if it's a Flight Academy flight
			ctx.setMessage("Checking for Flight Academy flight");
			GetSchedule sdao = new GetSchedule(con);
			ScheduleEntry sEntry = sdao.get(afr);
			boolean isAcademy = ((sEntry != null) && sEntry.getAcademy());
			afr.setAttribute(FlightReport.ATTR_ACADEMY, isAcademy);

			// Check the schedule database and check the route pair
			ctx.setMessage("Checking schedule for " + afr.getAirportD() + " to " + afr.getAirportA());
			int avgHours = sdao.getFlightTime(afr.getAirportD().getIATA(), afr.getAirportA().getIATA());
			if ((avgHours == 0) && (!info.isScheduleValidated())) {
				afr.setAttribute(FlightReport.ATTR_ROUTEWARN, true);
			} else if (avgHours > 0) {
				int minHours = (int) ((avgHours * 0.75) - (SystemData.getDouble("users.pirep.pad_hours", 0) * 10));
				int maxHours = (int) ((avgHours * 1.15) + (SystemData.getDouble("users.pirep.pad_hours", 0) * 10));
				if ((afr.getLength() < minHours) || (afr.getLength() > maxHours))
					afr.setAttribute(FlightReport.ATTR_TIMEWARN, true);
			}
			
			// Start the transaction
			ctx.startTX();

			// Get the position write DAO and write the positions
			if (info != null) {
				afr.setFSVersion(info.getFSVersion());
				if (afr.getDatabaseID(FlightReport.DBID_ACARS) == 0)
					afr.setDatabaseID(FlightReport.DBID_ACARS, info.getFlightID());
				
				// Mark the PIREP as filed
				SetInfo idao = new SetInfo(con);
				idao.logPIREP(info.getFlightID());
				info.setComplete(true);
			} else {
				afr.setFSVersion(2004);
				log.warn("No Flight Information found for ACARS Connection");
			}

			// Update the checkride record (don't assume pilots check the box, because they don't)
			if ((info != null) && afr.hasAttribute(FlightReport.ATTR_CHECKRIDE)) {
				GetExam exdao = new GetExam(con);
				CheckRide cr = exdao.getCheckRide(usrLoc.getDB(), usrLoc.getID(), afr.getEquipmentType(), Test.NEW);
				if (cr != null) {
					ctx.setMessage("Saving check ride data for ACARS Flight " + info.getFlightID());
					cr.setFlightID(info.getFlightID());
					cr.setSubmittedOn(new Date());
					cr.setStatus(Test.SUBMITTED);
					
					// Update the checkride
					SetExam wdao = new SetExam(con);
					wdao.write(usrLoc.getDB(), cr);
				} else {
					afr.setAttribute(FlightReport.ATTR_CHECKRIDE, false);
				}
			}

			// Get the write DAO and save the PIREP
			ctx.setMessage("Saving Flight report for flight " + afr.getFlightCode() + " for " + ac.getUserID());
			SetFlightReport wdao = new SetFlightReport(con);
			wdao.write(afr, usrLoc.getDB());
			wdao.writeACARS(afr, usrLoc.getDB());
			
			// Commit the transaction
			ctx.commitTX();
			
			// Log completion
			log.info("PIREP from " + env.getOwner().getName() + " (" + env.getOwnerID() + ") filed");
		} catch (Exception e) {
			ctx.rollbackTX();
			log.error(ac.getUserID() + " - " + e.getMessage(), e);
			ackMsg.setEntry("error", "PIREP Submission failed - " + e.getMessage());
		} finally {
			ctx.release();
			ctx.push(ackMsg, ac.getID(), true);
		}
	}
	
	/**
	 * Returns the maximum execution time of this command before a warning is issued.
	 * @return the maximum execution time in milliseconds
	 */
	public final int getMaxExecTime() {
		return 2500;
	}
}