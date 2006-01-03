// Copyright (c) 2004, 2005 Global Virtual Airline Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;
import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.beans.*;
import org.deltava.beans.testing.CheckRide;
import org.deltava.beans.system.UserData;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import org.deltava.dao.*;
import org.deltava.dao.acars.*;

import org.deltava.util.system.SystemData;

/**
 * An ACARS command to file a Flight Report.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class FilePIREPCommand implements ACARSCommand {

	private static final Logger log = Logger.getLogger(FilePIREPCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, Envelope env) {

		// Log PIREP filing
		log.info("Receiving PIREP from " + env.getOwner().getName() + " (" + env.getOwnerID() + ")");

		// Get the Message and the ACARS connection
		FlightReportMessage msg = (FlightReportMessage) env.getMessage();
		ACARSConnection ac = ctx.getACARSConnection();
		
		// Generate the response message
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(ac.getUser(), msg.getID());
		
		// Get the PIREP data and flight information
		ACARSFlightReport afr = msg.getPIREP();
		InfoMessage info = ac.getFlightInfo();
		UserData usrLoc = ac.getUserData();
		
		// If we have no flight info, then push it back
		if (info == null) {
			log.warn("No Flight Information found for ACARS Connection");
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
			List dFlights = prdao.getDraftReports(usrLoc.getID(), afr.getAirportD(), afr.getAirportA(), usrLoc.getDB());
			if (!dFlights.isEmpty()) {
				FlightReport fr = (FlightReport) dFlights.get(0);
				afr.setID(fr.getID());
				afr.setDatabaseID(FlightReport.DBID_ASSIGN, fr.getDatabaseID(FlightReport.DBID_ASSIGN));
				afr.setDatabaseID(FlightReport.DBID_EVENT, fr.getDatabaseID(FlightReport.DBID_EVENT));
			}

			// Check if this Flight Report counts for promotion
			ctx.setMessage("Checking type ratings for " + ac.getUserID());
			GetEquipmentType eqdao = new GetEquipmentType(con);
			Collection<String> promoEQ = eqdao.getPrimaryTypes(usrLoc.getDB(), afr.getEquipmentType());
			if (promoEQ.contains(ac.getUser().getEquipmentType()))
				afr.setCaptEQType(promoEQ);
			
			// Check if the user is rated to fly the aircraft
			if (!ac.getUser().getRatings().contains(afr.getEquipmentType()))
				afr.setAttribute(FlightReport.ATTR_NOTRATED, true);

			// Check the schedule database and check the route pair
			ctx.setMessage("Checking schedule for " + afr.getAirportD() + " to " + afr.getAirportA());
			GetSchedule sdao = new GetSchedule(con);
			int avgHours = sdao.getFlightTime(afr.getAirportD().getIATA(), afr.getAirportA().getIATA());
			if (avgHours == 0) {
				afr.setAttribute(FlightReport.ATTR_ROUTEWARN, true);
			} else {
				int minHours = (int) ((avgHours * 0.75) - (SystemData.getDouble("users.pirep.pad_hours", 0) * 10));
				int maxHours = (int) ((avgHours * 1.15) + (SystemData.getDouble("users.pirep.pad_hours", 0) * 10));
				if ((afr.getLength() < minHours) || (afr.getLength() > maxHours))
					afr.setAttribute(FlightReport.ATTR_TIMEWARN, true);
			}

			// Start the transaction
			con.setAutoCommit(false);

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

			// If we're a checkride, then update the checkride record
			if (afr.hasAttribute(FlightReport.ATTR_CHECKRIDE) && (info != null)) {
				GetExam exdao = new GetExam(con);
				CheckRide cr = exdao.getCheckRide(usrLoc.getDB(), usrLoc.getID(), afr.getEquipmentType());
				if (cr != null) {
					ctx.setMessage("Saving check ride data for ACARS Flight " + info.getFlightID());
					cr.setFlightID(info.getFlightID());
					cr.setSubmittedOn(new Date());
					
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
			con.commit();
			
			// Log completion
			log.info("PIREP from " + env.getOwner().getName() + " (" + env.getOwnerID() + ") filed");
		} catch (Exception e) {
			try {
				con.rollback();
			} catch (Exception e2) {
			} finally {
				log.error(e.getMessage(), e);
				ackMsg.setEntry("error", "PIREP Submission failed - " + e.getMessage());
			}
		} finally {
			ctx.release();
		}

		// Send the response
		ctx.push(ackMsg, ac.getID());
	}
}