// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;
import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.beans.*;
import org.deltava.beans.academy.*;
import org.deltava.beans.acars.*;
import org.deltava.beans.econ.*;
import org.deltava.beans.event.Event;
import org.deltava.beans.fb.*;
import org.deltava.beans.flight.*;
import org.deltava.beans.navdata.*;
import org.deltava.beans.testing.*;
import org.deltava.beans.schedule.*;
import org.deltava.beans.system.AirlineInformation;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import org.deltava.dao.*;
import org.deltava.dao.acars.*;
import org.deltava.dao.http.SetFacebookData;

import org.deltava.mail.*;
import org.deltava.util.*;
import org.deltava.util.system.SystemData;

import org.gvagroup.common.*;

/**
 * An ACARS Server command to file a Flight Report.
 * @author Luke
 * @version 5.0
 * @since 1.0
 */

public class FilePIREPCommand extends ACARSCommand {

	private static final Logger log = Logger.getLogger(FilePIREPCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
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

		int flightID = info.getFlightID();
		Connection con = null;
		try {
			con = ctx.getConnection();
			GetFlightReportACARS prdao = new GetFlightReportACARS(con);
			GetACARSPositions fddao = new GetACARSPositions(con);

			// Check for existing PIREP with this flight ID
			ctx.setMessage("Checking for duplicate Flight Report from " + ac.getUserID());
			if (flightID != 0) {
				FDRFlightReport afr2 = prdao.getACARS(usrLoc.getDB(), flightID);
				if (afr2 != null) {
					ctx.release();	

					// Save the PIREP ID in the ACK message
					ackMsg.setEntry("domain", usrLoc.getDomain());
					ackMsg.setEntry("pirepID", afr2.getHexID());
					
					// Log warning and return an ACK
					log.warn("Flight " + flightID + " already has PIREP");
					ctx.push(ackMsg, ac.getID());
					return;
				}
				
				// Check for flight ID filed around the same time
				int dupeID = fddao.getDuplicateID(flightID);
				if (dupeID > 0)
					afr2 = prdao.getACARS(usrLoc.getDB(), dupeID);
				
				// If it has a PIREP, ACK with that PIREP's ID. If it doesn't, use that flight ID for this PIREP.
				if (afr2 != null) {
					ctx.release();
					
					// Save the PIREP ID in the ACK message
					ackMsg.setEntry("domain", usrLoc.getDomain());
					ackMsg.setEntry("pirepID", afr2.getHexID());

					// Log warning and return an ACK
					log.warn("Ignoring duplicate PIREP submission from " + ac.getUserID() + ", ID = " + dupeID);
					ctx.push(ackMsg, ac.getID());
					return;
				}
				
				// Use the original flight ID
				afr.setDatabaseID(DatabaseID.ACARS, dupeID);
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

			// Flush the position queue
			if (SetPosition.size() > 0) {
				ctx.setMessage("Flushing Position Queue");
				SetPosition pwdao = new SetPosition(con);
				int flushed = pwdao.flush();
				log.info("Flushed " + flushed + " Position records from queue");
			}
			
			// Create comments field
			Collection<String> comments = new LinkedHashSet<String>();

			// If we found a draft flight report, save its database ID and copy its ID to the PIREP we will file
			ctx.setMessage("Checking for draft Flight Reports by " + ac.getUserID());
			List<FlightReport> dFlights = prdao.getDraftReports(usrLoc.getID(), afr.getAirportD(), afr.getAirportA(), usrLoc.getDB());
			if (!dFlights.isEmpty()) {
				FlightReport fr = dFlights.get(0);
				afr.setID(fr.getID());
				afr.setDatabaseID(DatabaseID.ASSIGN, fr.getDatabaseID(DatabaseID.ASSIGN));
				afr.setDatabaseID(DatabaseID.EVENT, fr.getDatabaseID(DatabaseID.EVENT));
				afr.setAttribute(FlightReport.ATTR_CHARTER, fr.hasAttribute(FlightReport.ATTR_CHARTER));
				if (!StringUtils.isEmpty(fr.getComments()))
					comments.add(fr.getComments());
			}

			// Reload the User
			GetPilotDirectory pdao = new GetPilotDirectory(con);
			GetPilot.invalidateID(usrLoc.getID());
			Pilot p = pdao.get(usrLoc);

			// Add user data
			afr.setDatabaseID(DatabaseID.PILOT, p.getID());
			afr.setRank(p.getRank());

			// Convert the date into the user's local time zone
			DateTime dt = new DateTime(afr.getDate());
			dt.convertTo(p.getTZ());
			afr.setDate(dt.getDate());

			// Check that the user has an online network ID
			OnlineNetwork network = afr.getNetwork();
			if ((network != null) && (!p.hasNetworkID(network))) {
				log.info(p.getName() + " does not have a " + network.toString() + " ID");
				comments.add("No " + network.toString() + " ID, resetting Online Network flag");
				afr.setNetwork(null);
				afr.setDatabaseID(DatabaseID.EVENT, 0);
			} else if ((network == null) && (afr.getDatabaseID(DatabaseID.EVENT) != 0))
				afr.setDatabaseID(DatabaseID.EVENT, 0);
			
			// Check if it's an Online Event flight
			GetEvent evdao = new GetEvent(con);
			if ((afr.getDatabaseID(DatabaseID.EVENT) == 0) && (afr.hasAttribute(FlightReport.ATTR_ONLINE_MASK)))
				afr.setDatabaseID(DatabaseID.EVENT, evdao.getEvent(afr.getAirportD(), afr.getAirportA(), network));

			// Check that it was submitted in time
			if (afr.getDatabaseID(DatabaseID.EVENT) != 0) {
				Event e = evdao.get(afr.getDatabaseID(DatabaseID.EVENT));
				if (e != null) {
					long timeSinceEnd = (System.currentTimeMillis() - e.getEndTime().getTime()) / 1000;
					if (timeSinceEnd > 21600) {
						comments.add("SYSTEM: Flight logged over 6 hours after Event completion");
						afr.setDatabaseID(DatabaseID.EVENT, 0);
					}
				} else
					afr.setDatabaseID(DatabaseID.EVENT, 0);
			}
			
			// Load the aircraft
			GetAircraft acdao = new GetAircraft(con);
			Aircraft a = acdao.get(afr.getEquipmentType());
			if (a == null) 
				throw new DAOException("Invalid equipment type - " + afr.getEquipmentType());
			else if (!a.getName().equals(afr.getEquipmentType()))
				throw new DAOException("Expected " + afr.getEquipmentType() + ", received " + a.getName());

			// Check if this Flight Report counts for promotion
			ctx.setMessage("Checking type ratings for " + ac.getUserID());
			GetEquipmentType eqdao = new GetEquipmentType(con);
			EquipmentType eq = eqdao.get(p.getEquipmentType(), usrLoc.getDB());
			Collection<String> promoEQ = eqdao.getPrimaryTypes(usrLoc.getDB(), afr.getEquipmentType());
			
			// Loop through the eq types, not all may have the same minimum promotion stage length!!
			if (promoEQ.contains(p.getEquipmentType())) {
				FlightPromotionHelper helper = new FlightPromotionHelper(afr);
				for (Iterator<String> i = promoEQ.iterator(); i.hasNext();) {
					String pType = i.next();
					EquipmentType pEQ = eqdao.get(pType, usrLoc.getDB());
					boolean isOK = helper.canPromote(pEQ);
					if (!isOK) {
						i.remove();
						if (!StringUtils.isEmpty(helper.getLastComment()))
							comments.add("SYSTEM: Not eligible for promotion: " + helper.getLastComment());
					}
				}

				afr.setCaptEQType(promoEQ);
			}
			
			// Check if the user is rated to fly the aircraft
			afr.setAttribute(FlightReport.ATTR_HISTORIC, a.getHistoric());
			if (!p.getRatings().contains(afr.getEquipmentType()) && !eq.getRatings().contains(afr.getEquipmentType())) {
				log.warn(p.getName() + " not rated in " + afr.getEquipmentType() + " ratings = " + p.getRatings());
				afr.setAttribute(FlightReport.ATTR_NOTRATED, !afr.hasAttribute(FlightReport.ATTR_CHECKRIDE));
			}

			// Check for excessive distance
			if (afr.getDistance() > a.getRange())
				afr.setAttribute(FlightReport.ATTR_RANGEWARN, true);

			// Check for excessive weight
			if ((a.getMaxTakeoffWeight() != 0) && (afr.getTakeoffWeight() > a.getMaxTakeoffWeight()))
				afr.setAttribute(FlightReport.ATTR_WEIGHTWARN, true);
			else if ((a.getMaxLandingWeight() != 0) && (afr.getLandingWeight() > a.getMaxLandingWeight()))
				afr.setAttribute(FlightReport.ATTR_WEIGHTWARN, true);
			
			// Check ETOPS
			Collection<? extends GeoLocation> rtEntries = fddao.getRouteEntries(flightID, false, false);
			ETOPSResult etopsClass = ETOPSHelper.classify(rtEntries);
			afr.setAttribute(FlightReport.ATTR_ETOPSWARN, ETOPSHelper.validate(a, etopsClass.getResult()));
			if (afr.hasAttribute(FlightReport.ATTR_ETOPSWARN))
				comments.add("SYSTEM: ETOPS classificataion: " + String.valueOf(etopsClass));
			
			// Calculate flight load factor if not set client-side
			java.io.Serializable econ = SharedData.get(SharedData.ECON_DATA + usrLoc.getAirlineCode());
			if (econ != null) {
				if (afr.getLoadFactor() == 0) {
					ctx.setMessage("Calculating flight load factor");
					LoadFactor lf = new LoadFactor((EconomyInfo) IPCUtils.reserialize(econ));
					double loadFactor = lf.generate(afr.getDate());
					afr.setLoadFactor(loadFactor);
				}
				
				if ((a.getSeats() > 0) && (afr.getPassengers() == 0)) {
					int paxCount = (int) Math.round(a.getSeats() * afr.getLoadFactor());
					afr.setPassengers(Math.min(a.getSeats(), paxCount));
					if (paxCount > a.getSeats())
						log.warn("Invalid passenger count - pax=" + paxCount + ", seats=" + a.getSeats());
				}
			}

			// Check for in-flight refueling
			ctx.setMessage("Checking for In-Flight Refueling");
			FuelUse use = fddao.checkRefuel(flightID);
			afr.setTotalFuel(use.getTotalFuel());
			afr.setAttribute(FlightReport.ATTR_REFUELWARN, use.getRefuel());

			// Check if it's a Flight Academy flight
			ctx.setMessage("Checking for Flight Academy flight");
			GetSchedule sdao = new GetSchedule(con);
			ScheduleEntry sEntry = sdao.get(afr, usrLoc.getDB());
			boolean isAcademy = ((sEntry != null) && sEntry.getAcademy());
			
			// If we're an Academy flight, check if we have an active course
			Course c = null;
			if (isAcademy) {
				GetAcademyCourses crsdao = new GetAcademyCourses(con);
				Collection<Course> courses = crsdao.getByPilot(usrLoc.getID());
				for (Iterator<Course> i = courses.iterator(); (c == null) && i.hasNext(); ) {
					Course crs = i.next();
					if (crs.getStatus() == Status.STARTED)
						c = crs;
				}
				
				boolean isINS = p.isInRole("Instructor") ||  p.isInRole("AcademyAdmin") || p.isInRole("AcademyAudit") || p.isInRole("Examiner");
				afr.setAttribute(FlightReport.ATTR_ACADEMY, (c != null) || isINS);	
			}

			// Check the schedule database and check the route pair
			ctx.setMessage("Checking schedule for " + afr.getAirportD() + " to " + afr.getAirportA());
			boolean isAssignment = (afr.getDatabaseID(DatabaseID.ASSIGN) != 0);
			boolean isEvent = (afr.getDatabaseID(DatabaseID.EVENT) != 0);
			FlightTime avgHours = sdao.getFlightTime(afr, usrLoc.getDB());
			if ((avgHours.getFlightTime() == 0) && (!isAcademy && !isAssignment && !isEvent)) {
				log.warn("No flights found between " + afr.getAirportD() + " and " + afr.getAirportA());
				boolean wasValid = info.isScheduleValidated() && info.matches(afr.getAirportD(), afr.getAirportA());
				if (!wasValid)
					afr.setAttribute(FlightReport.ATTR_ROUTEWARN, !afr.hasAttribute(FlightReport.ATTR_CHARTER));
			} else {
				int minHours = (int) ((avgHours.getFlightTime() * 0.75) - 5); // fixed 0.5 hour
				int maxHours = (int) ((avgHours.getFlightTime() * 1.15) + 5);
				if ((afr.getLength() < minHours) || (afr.getLength() > maxHours))
					afr.setAttribute(FlightReport.ATTR_TIMEWARN, true);
			}

			// Load held PIREP count
			ctx.setMessage("Checking Held Flight Reports for " + ac.getUserID());
			int heldPIREPs = prdao.getHeld(usrLoc.getID(), usrLoc.getDB());
			if (heldPIREPs >= SystemData.getInt("users.pirep.maxHeld", 5)) {
				comments.add("SYSTEM: Automatically Held due to " + heldPIREPs + " held Flight Reports");
				afr.setStatus(FlightReport.HOLD);
			}

			// If we don't have takeoff/touchdown points from Build 100+, derive them
			GetNavAirway navdao = new GetNavAirway(con);
			if (afr.getTakeoffHeading() == -1) {
				List<? extends RouteEntry> tdEntries = fddao.getTakeoffLanding(flightID, false);
				if (tdEntries.size() > 2) {
					int ofs = 0;
					ACARSRouteEntry entry = (ACARSRouteEntry) tdEntries.get(0);
					GeoPosition adPos = new GeoPosition(info.getAirportD());
					while ((ofs < (tdEntries.size() - 1)) && (adPos.distanceTo(entry) < 15) && (entry.getVerticalSpeed() > 0)) {
						ofs++;
						entry = (ACARSRouteEntry) tdEntries.get(ofs);
					}

					// Trim out spurious takeoff entries
					if (ofs > 0)
						tdEntries.subList(0, ofs - 1).clear();
					if (tdEntries.size() > 2)
						tdEntries.subList(1, tdEntries.size() - 1).clear();
				}
				
				// Save the entry points
				if (tdEntries.size() > 0) {
					afr.setTakeoffLocation(tdEntries.get(0));
					afr.setTakeoffHeading(tdEntries.get(0).getHeading());
					if (tdEntries.size() > 1) {
						afr.setLandingLocation(tdEntries.get(1));
						afr.setLandingHeading(tdEntries.get(1).getHeading());
					}
				}
			}

			// Load the departure runway
			Runway rD = null;
			if (afr.getTakeoffHeading() > -1) {
				LandingRunways lr = navdao.getBestRunway(info.getAirportD(), afr.getFSVersion(), afr.getTakeoffLocation(), afr.getTakeoffHeading());
				Runway r = lr.getBestRunway();
				if (r != null) {
					int dist = GeoUtils.distanceFeet(r, afr.getTakeoffLocation());
					rD = new RunwayDistance(r, dist);
					if (r.getLength() < a.getTakeoffRunwayLength())
						afr.setAttribute(FlightReport.ATTR_RWYWARN, true);
				}
			}

			// Load the arrival runway
			Runway rA = null;
			if (afr.getLandingHeading() > -1) {
				LandingRunways lr = navdao.getBestRunway(afr.getAirportA(), afr.getFSVersion(), afr.getLandingLocation(), afr.getLandingHeading());
				Runway r = lr.getBestRunway();
				if (r != null) {
					int dist = GeoUtils.distanceFeet(r, afr.getLandingLocation());
					rA = new RunwayDistance(r, dist);
					if (r.getLength() < a.getLandingRunwayLength())
						afr.setAttribute(FlightReport.ATTR_RWYWARN, true);
				}
			}
			
			// Get framerates
			afr.setAverageFrameRate(fddao.getFrameRate(flightID));
			
			// Set misc options
			afr.setClientBuild(ac.getClientBuild());
			afr.setBeta(ac.getBeta());
			afr.setAttribute(FlightReport.ATTR_DISPATCH, info.isDispatchPlan());
			afr.setFSVersion(info.getFSVersion());
			if (afr.getDatabaseID(DatabaseID.ACARS) == 0)
				afr.setDatabaseID(DatabaseID.ACARS, flightID);

			// Start the transaction
			ctx.startTX();

			// Mark the PIREP as filed
			SetInfo idao = new SetInfo(con);
			idao.logPIREP(flightID);
			info.setComplete(true);
			
			// Update the checkride record (don't assume pilots check the box, because they don't)
			CheckRide cr = null;
			if (afr.hasAttribute(FlightReport.ATTR_CHECKRIDE)) {
				GetExam exdao = new GetExam(con);
				// Check for Academy chck ride
				if (c != null) {
					List<CheckRide> rides = exdao.getAcademyCheckRides(c.getID(), TestStatus.NEW);
					if (!rides.isEmpty())
						cr = rides.get(0);
				}
				
				// Get check ride
				if (cr == null)
					cr = exdao.getCheckRide(usrLoc.getID(), afr.getEquipmentType(), TestStatus.NEW);
				
				if (cr != null) {
					ctx.setMessage("Saving check ride data for ACARS Flight " + flightID);
					cr.setFlightID(info.getFlightID());
					cr.setSubmittedOn(new Date());
					cr.setStatus(TestStatus.SUBMITTED);
					if (cr.getAcademy() && !afr.hasAttribute(FlightReport.ATTR_ACADEMY))
						afr.setAttribute(FlightReport.ATTR_ACADEMY, true);

					// Update the checkride
					SetExam wdao = new SetExam(con);
					wdao.write(cr);
				} else
					afr.setAttribute(FlightReport.ATTR_CHECKRIDE, false);
			}

			// Write the runway data
			SetACARSData awdao = new SetACARSData(con);
			awdao.writeRunways(flightID, rD, rA);
			
			// Check if we're a dispatch plan
			if (msg.isDispatch() && !info.isDispatchPlan()) {
				log.warn("Flight " + flightID + " was not set as Dispatch, but PIREP has Dispatch flag!");
				afr.setAttribute(FlightReport.ATTR_DISPATCH, true);
				
				// Validate the dispatch route ID
				GetACARSRoute ardao = new GetACARSRoute(con);
				DispatchRoute dr = ardao.getRoute(msg.getRouteID());
				if (dr == null) {
					log.warn("Invalid Dispatch Route - " + msg.getRouteID());
					msg.setRouteID(0);
				} else
					awdao.writeDispatch(flightID, msg.getDispatcherID(), msg.getRouteID());
			}

			// Parse the route and check for actual SID/STAR
			List<String> wps = StringUtils.split(info.getRoute(), " ");
			wps.remove(info.getAirportD().getICAO());
			wps.remove(info.getAirportA().getICAO());
			if (wps.size() > 2) {
				ctx.setMessage("Checking actual SID/STAR for ACARS Flight " + flightID);

				// Check actual SID/STAR
				TerminalRoute aSID = navdao.getBestRoute(afr.getAirportD(), TerminalRoute.SID, wps.get(0), wps.get(1), rD);
				if ((aSID != null) && (!aSID.getCode().equals(info.getSID()))) {
					awdao.clearSID(flightID);
					awdao.writeSIDSTAR(flightID, aSID);
					if (ac.getVersion() > 2)
						comments.add("SYSTEM: Filed SID was " + info.getSID() + ", actual was " + aSID.getCode());
				}

				TerminalRoute aSTAR = navdao.getBestRoute(afr.getAirportA(), TerminalRoute.STAR, wps.get(wps.size() - 1), wps.get(wps.size() - 2), rA);
				if (aSTAR == null)
					aSTAR = navdao.getBestRoute(afr.getAirportA(), TerminalRoute.STAR, wps.get(wps.size() - 1), null, rA); 
				if ((aSTAR != null) && (!aSTAR.getCode().equals(info.getSTAR()))) {
					awdao.clearSTAR(flightID);
					awdao.writeSIDSTAR(flightID, aSTAR);
					if (ac.getVersion() > 2)
						comments.add("SYSTEM: Filed STAR was " + info.getSTAR() + ", actual was " + aSTAR.getCode());
				}
			}
			
			// Save comments
			if (!comments.isEmpty())
				afr.setComments(StringUtils.listConcat(comments, "\r\n"));
			
			// Get the write DAO and save the PIREP
			ctx.setMessage("Saving Flight report for flight " + afr.getFlightCode() + " for " + ac.getUserID());
			SetFlightReport wdao = new SetFlightReport(con);
			wdao.write(afr, usrLoc.getDB());
			wdao.writeACARS(afr, usrLoc.getDB());
			if (wdao.updatePaxCount(afr.getID(), usrLoc.getDB()))
				log.warn("Update Passnger count for PIREP #" + afr.getID());

			// Commit the transaction
			ctx.commitTX();

			// Save the PIREP ID in the ACK message and send the ACK
			ackMsg.setEntry("pirepID", afr.getHexID());
			ackMsg.setEntry("domain", usrLoc.getDomain());
			ctx.push(ackMsg, ac.getID(), true);
			
			// Send a notification message if a check ride, either to the CP/ACP, or the Instructor(s)
			GetMessageTemplate mtdao = new GetMessageTemplate(con);
			if ((cr != null) && afr.hasAttribute(FlightReport.ATTR_ACADEMY)) {
				ctx.setMessage("Sending Flight Academy check ride notification");
				if (c != null) {
					MessageContext mctxt = new MessageContext();
					mctxt.addData("user", p);
					mctxt.addData("pirep", afr);
					mctxt.addData("airline", SystemData.getApp(usrLoc.getAirlineCode()).getName());
					mctxt.addData("url", "http://www." + usrLoc.getDomain() + "/");
					
					// Load the template
					mctxt.setTemplate(mtdao.get(usrLoc.getDB(), "CRSUBMIT"));
					
					// Get the Instructor(s)
					GetUserData uddao = new GetUserData(con);
					Collection<Pilot> insList = new TreeSet<Pilot>();
					if (c.getInstructorID() != 0) {
						Pilot ins = pdao.get(uddao.get(c.getInstructorID()));
						if (ins != null)
							insList.add(ins);
					}
					
					if (insList.isEmpty()) {
						for (AirlineInformation ai : uddao.getAirlines(false).values())
							insList.addAll(pdao.getByRole("Instructor", ai.getDB()));
					}
					
					// Send the message to the Instructors
					Mailer mailer = new Mailer(p);
					mailer.setContext(mctxt);
					mailer.send(insList);
				}
			} else if (afr.hasAttribute(FlightReport.ATTR_CHECKRIDE) && (cr != null)) {
				ctx.setMessage("Sending check ride notification");
				EquipmentType crEQ = eqdao.get(cr.getEquipmentType(), cr.getOwner().getDB());
				if (crEQ != null) {
					MessageContext mctxt = new MessageContext();
					mctxt.addData("user", p);
					mctxt.addData("pirep", afr);
					mctxt.addData("airline", crEQ.getOwner().getName());
					mctxt.addData("url", "http://www." + eq.getOwner().getDomain() + "/");
					
					// Load the template
					mctxt.setTemplate(mtdao.get(crEQ.getOwner().getDB(), "CRSUBMIT"));

					// Load the equipment type ACPs
					Collection<Pilot> eqACPs = pdao.getPilotsByEQ(crEQ, null, true, Rank.ACP);

					// Send the message to the CP
					Mailer mailer = new Mailer(p);
					mailer.setContext(mctxt);
					for (Pilot acp : eqACPs)
						mailer.setCC(acp);
					mailer.send(Mailer.makeAddress(crEQ.getCPEmail(), crEQ.getCPName()));
				}
			}
			
			// Post Facebook notification
			Object rawCreds = SharedData.get(SharedData.FB_CREDS + usrLoc.getAirlineCode());
			if ((rawCreds != null) && (p.hasIM(IMAddress.FBTOKEN))) {
				ctx.setMessage("Posting to Facebook");
				
				// Build the message
				String baseURL = "http://www." + usrLoc.getDomain() + "/";
				MessageContext mctxt = new MessageContext();
				mctxt.addData("user", p);
				mctxt.addData("airline", SystemData.getApp(usrLoc.getAirlineCode()).getName());
				mctxt.addData("url", baseURL);
				mctxt.addData("pirep", afr);
				
				// Load the template and generate the body text
				mctxt.setTemplate(mtdao.get(usrLoc.getDB(), "FBPIREP"));
				NewsEntry nws = new NewsEntry(mctxt.getBody(), baseURL + "pirep.do?id=" + afr.getHexID());
				nws.setLinkCaption(afr.getFlightCode());
				ctx.release();
				
				// Post to user's feed
				SetFacebookData fbwdao = new SetFacebookData();
				fbwdao.setWarnMode(true);
				fbwdao.setToken(ac.getUser().getIMHandle(IMAddress.FBTOKEN));
				fbwdao.write(nws);
			}

			// Log completion
			log.info("PIREP from " + env.getOwner().getName() + " (" + env.getOwnerID() + ") filed");
		} catch (DAOException de) {
			ctx.rollbackTX();
			log.error(ac.getUserID() + " - " + de.getMessage(), de);
			ackMsg.setEntry("error", "PIREP Submission failed - " + de.getMessage());
			ctx.push(ackMsg, ac.getID(), true);
		} finally {
			ctx.release();
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