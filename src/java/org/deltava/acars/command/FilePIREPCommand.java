// Copyright 2004, 2005, 2006, 2007, 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;
import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.beans.*;
import org.deltava.beans.testing.*;
import org.deltava.beans.schedule.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import org.deltava.dao.*;
import org.deltava.dao.acars.*;

import org.deltava.util.*;
import org.deltava.util.system.SystemData;

/**
 * An ACARS command to file a Flight Report.
 * @author Luke
 * @version 2.2
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
			GetFlightReportACARS prdao = new GetFlightReportACARS(con);
			int flightID = info.getFlightID();
			
			// Check for existing PIREP with this flight ID
			ctx.setMessage("Checking for duplicate Flight Report from " + ac.getUserID());
			if (flightID != 0) {
			   ACARSFlightReport afr2 = prdao.getACARS(usrLoc.getDB(), info.getFlightID());
			   if (afr2 != null) {
			      ctx.release();
			      
			      // Log warning and return an ACK
			      log.warn("Ignoring duplicate PIREP submission from " + ac.getUserID());
			      ctx.push(ackMsg, ac.getID());
			      return;
			   }
			} else {
				List<FlightReport> dupes = prdao.checkDupes(usrLoc.getDB(), afr, usrLoc.getID());
				dupes.addAll(prdao.checkDupes(usrLoc.getDB(), flightID));
				if (dupes.size() > 0) {
					ctx.release();

			      // Log warning and return an ACK
			      log.warn("Ignoring possible duplicate PIREP from " + ac.getUserID());
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
				afr.setAttribute(FlightReport.ATTR_CHARTER, fr.hasAttribute(FlightReport.ATTR_CHARTER));
				afr.setComments(fr.getComments());
			}
			
			// Check if it's an Online Event flight
			if ((afr.getDatabaseID(FlightReport.DBID_EVENT) == 0) && (afr.hasAttribute(FlightReport.ATTR_ONLINE_MASK))) {
				OnlineNetwork network = OnlineNetwork.VATSIM;
				if (afr.hasAttribute(FlightReport.ATTR_IVAO))
					network = OnlineNetwork.IVAO;
				
				// Load the event ID
				GetEvent evdao = new GetEvent(con);
				afr.setDatabaseID(FlightReport.DBID_EVENT, evdao.getEvent(afr.getAirportD(), afr.getAirportA(), network));
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
			if (!p.getRatings().contains(afr.getEquipmentType()) && !eq.getRatings().contains(afr.getEquipmentType())) {
				log.warn(p.getName() + " not rated in " + afr.getEquipmentType() + " ratings = " + p.getRatings());
				afr.setAttribute(FlightReport.ATTR_NOTRATED, !afr.hasAttribute(FlightReport.ATTR_CHECKRIDE));
			}
			
			// Check for historic aircraft
			GetAircraft acdao = new GetAircraft(con);
			Aircraft a = acdao.get(afr.getEquipmentType());
			afr.setAttribute(FlightReport.ATTR_HISTORIC, (a != null) && (a.getHistoric()));
			if (a == null) {
				log.warn("Invalid equipment type from " + p.getName() + " - " + afr.getEquipmentType());
				afr.setEquipmentType(p.getEquipmentType());
			} else {
				// Check for excessive distance
				if (afr.getDistance() > a.getRange())
					afr.setAttribute(FlightReport.ATTR_RANGEWARN, true);
			
				// Check for excessive weight
				if ((a.getMaxTakeoffWeight() != 0) && (afr.getTakeoffWeight() > a.getMaxTakeoffWeight())) 
					afr.setAttribute(FlightReport.ATTR_WEIGHTWARN, true);
				else if ((a.getMaxLandingWeight()  != 0) && (afr.getLandingWeight() > a.getMaxLandingWeight()))
					afr.setAttribute(FlightReport.ATTR_WEIGHTWARN, true);
			}
			
			// Check for in-flight refueling
			ctx.setMessage("Checking for In-Flight Refueling");
			GetACARSData fddao = new GetACARSData(con);
			afr.setAttribute(FlightReport.ATTR_REFUELWARN, fddao.checkRefuel(flightID, false));
			
			// Check if it's a Flight Academy flight
			ctx.setMessage("Checking for Flight Academy flight");
			GetSchedule sdao = new GetSchedule(con);
			ScheduleEntry sEntry = sdao.get(afr, usrLoc.getDB());
			boolean isAcademy = ((sEntry != null) && sEntry.getAcademy());
			afr.setAttribute(FlightReport.ATTR_ACADEMY, isAcademy);
			
			// Check the schedule database and check the route pair
			ctx.setMessage("Checking schedule for " + afr.getAirportD() + " to " + afr.getAirportA());
			boolean isAssignment = (afr.getDatabaseID(FlightReport.DBID_ASSIGN) != 0);
			int avgHours = sdao.getFlightTime(afr.getAirportD(), afr.getAirportA(), usrLoc.getDB());
			if ((avgHours == 0) && (!isAcademy && !isAssignment)) {
				log.warn("No flights found between " + afr.getAirportD() + " and " + afr.getAirportA());
				boolean wasValid = info.isScheduleValidated() && info.matches(afr.getAirportD(), afr.getAirportA()); 
				if (!wasValid)
					afr.setAttribute(FlightReport.ATTR_ROUTEWARN, !afr.hasAttribute(FlightReport.ATTR_CHARTER));
			} else {
				int minHours = (int) ((avgHours * 0.75) - 5); // fixed 0.5 hour pad
				int maxHours = (int) ((avgHours * 1.15) + 5);
				if ((afr.getLength() < minHours) || (afr.getLength() > maxHours))
					afr.setAttribute(FlightReport.ATTR_TIMEWARN, true);
			}
			
			// Load held PIREP count
			ctx.setMessage("Checking Held Flight Reports for " + ac.getUserID());
			int heldPIREPs = prdao.getHeld(usrLoc.getID(), usrLoc.getDB());
			if (heldPIREPs >= SystemData.getInt("users.pirep.maxHeld", 5)) {
				afr.setComments("Automatically Held due to " + heldPIREPs + " held Flight Reports");
				afr.setStatus(FlightReport.HOLD);
			}
			
			// Get the position write DAO and write the positions
			afr.setAttribute(FlightReport.ATTR_DISPATCH, info.isDispatchPlan());
			afr.setFSVersion(info.getFSVersion());
			if (afr.getDatabaseID(FlightReport.DBID_ACARS) == 0)
				afr.setDatabaseID(FlightReport.DBID_ACARS, info.getFlightID());
			
			// Start the transaction
			ctx.startTX();
				
			// Mark the PIREP as filed
			SetInfo idao = new SetInfo(con);
			idao.logPIREP(info.getFlightID());
			info.setComplete(true);

			// Update the checkride record (don't assume pilots check the box, because they don't)
			if (afr.hasAttribute(FlightReport.ATTR_CHECKRIDE)) {
				GetExam exdao = new GetExam(con);
				CheckRide cr = exdao.getCheckRide(usrLoc.getID(), afr.getEquipmentType(), Test.NEW);
				if (cr != null) {
					ctx.setMessage("Saving check ride data for ACARS Flight " + info.getFlightID());
					cr.setFlightID(info.getFlightID());
					cr.setSubmittedOn(new Date());
					cr.setStatus(Test.SUBMITTED);
					
					// Update the checkride
					SetExam wdao = new SetExam(con);
					wdao.write(cr);
				} else
					afr.setAttribute(FlightReport.ATTR_CHECKRIDE, false);
			}

			// Get the write DAO and save the PIREP
			ctx.setMessage("Saving Flight report for flight " + afr.getFlightCode() + " for " + ac.getUserID());
			SetFlightReport wdao = new SetFlightReport(con);
			wdao.write(afr, usrLoc.getDB());
			wdao.writeACARS(afr, usrLoc.getDB());
			
			// Commit the transaction
			ctx.commitTX();
			
			// Save the PIREP ID in the ACK message
			ackMsg.setEntry("pirepID", afr.getHexID());
			ackMsg.setEntry("domain", usrLoc.getDomain());
			
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