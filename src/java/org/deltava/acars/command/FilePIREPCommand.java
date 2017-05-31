// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2014, 2015, 2106, 2017 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;
import java.time.*;
import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.beans.*;
import org.deltava.beans.academy.*;
import org.deltava.beans.acars.*;
import org.deltava.beans.econ.*;
import org.deltava.beans.event.Event;
import org.deltava.beans.flight.*;
import org.deltava.beans.navdata.*;
import org.deltava.beans.testing.*;
import org.deltava.beans.schedule.*;
import org.deltava.beans.system.AirlineInformation;

import org.deltava.comparators.GeoComparator;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import org.deltava.dao.*;
import org.deltava.dao.acars.*;
import org.deltava.dao.redis.SetTrack;

import org.deltava.mail.*;
import org.deltava.util.*;
import org.deltava.util.cache.CacheManager;
import org.deltava.util.system.SystemData;

import org.gvagroup.common.*;

/**
 * An ACARS Server command to file a Flight Report.
 * @author Luke
 * @version 7.3
 * @since 1.0
 */

public class FilePIREPCommand extends PositionCacheCommand {

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
		AirlineInformation usrAirline = SystemData.getApp(usrLoc.getAirlineCode());

		// Adjust the times
		afr.setStartTime(afr.getStartTime().plusMillis(ac.getTimeOffset()));
		afr.setTaxiTime(afr.getTaxiTime().plusMillis(ac.getTimeOffset()));
		afr.setTakeoffTime(afr.getTakeoffTime().plusMillis(ac.getTimeOffset()));
		afr.setLandingTime(afr.getLandingTime().plusMillis(ac.getTimeOffset()));
		afr.setEndTime(afr.getEndTime().plusMillis(ac.getTimeOffset()));

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
			ctx.setMessage("Flushing Position Queue");
			int flushed = flush(true, ctx);
			if (flushed > 0)
				log.info("Flushed " + flushed + " Position records from queue");
			
			// Create comments field
			Collection<String> comments = new LinkedHashSet<String>();

			// If we found a draft flight report, save its database ID and copy its ID to the PIREP we will file
			ctx.setMessage("Checking for draft Flight Reports by " + ac.getUserID());
			List<FlightReport> dFlights = prdao.getDraftReports(usrLoc.getID(), afr, usrLoc.getDB());
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
			CacheManager.invalidate("Pilots", usrLoc.cacheKey());
			Pilot p = pdao.get(usrLoc);

			// Add user data
			afr.setDatabaseID(DatabaseID.PILOT, p.getID());
			afr.setRank(p.getRank());
			afr.setSimulator(info.getSimulator());

			// Convert the date into the user's local time zone
			ZonedDateTime zdt = ZonedDateTime.ofInstant(afr.getDate(), p.getTZ().getZone());
			afr.setDate(zdt.toInstant());

			// Check that the user has an online network ID
			OnlineNetwork network = afr.getNetwork();
			if ((network != null) && (!p.hasNetworkID(network))) {
				log.info(p.getName() + " does not have a " + network.toString() + " ID");
				comments.add("SYSTEM: No " + network.toString() + " ID, resetting Online Network flag");
				afr.setNetwork(null);
				afr.setDatabaseID(DatabaseID.EVENT, 0);
			} else if ((network == null) && (afr.getDatabaseID(DatabaseID.EVENT) != 0)) {
				comments.add("SYSTEM: Filed offline, resetting Online Event flag");
				afr.setDatabaseID(DatabaseID.EVENT, 0);
			}
			
			// Check if it's an Online Event flight
			GetEvent evdao = new GetEvent(con);
			if ((afr.getDatabaseID(DatabaseID.EVENT) == 0) && (network != null)) {
				int eventID = evdao.getPossibleEvent(afr);
				if (eventID != 0) {
					Event e = evdao.get(eventID);
					comments.add("SYSTEM: Detected participation in " + e.getName() + " Online Event");
					afr.setDatabaseID(DatabaseID.EVENT, eventID);
				}
			}

			// Check that it was submitted in time
			Event e = evdao.get(afr.getDatabaseID(DatabaseID.EVENT));
			if (e != null) {
				long timeSinceEnd = (System.currentTimeMillis() - e.getEndTime().toEpochMilli()) / 3600_000;
				if (timeSinceEnd > 6) {
					comments.add("SYSTEM: Flight logged " + timeSinceEnd + " hours after '" + e.getName() + "' completion");
					afr.setDatabaseID(DatabaseID.EVENT, 0);
				}
			} else if (afr.getDatabaseID(DatabaseID.EVENT) != 0) {
				comments.add("SYSTEM: Unknown Online Event - " + afr.getDatabaseID(DatabaseID.EVENT));
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
					if (pEQ == null)
						log.warn("Cannot find " + pType + " in " + usrLoc.getDB());
					
					boolean isOK = helper.canPromote(pEQ);
					if (!isOK) {
						i.remove();
						log.info(p.getName() + " leg not eligible for promotion in " + pEQ + ": " + helper.getLastComment());
						comments.add("SYSTEM: Not eligible for promotion: " + helper.getLastComment());
					}
				}

				afr.setCaptEQType(promoEQ);
				log.info("Setting Promotion Flags for " + env.getOwner().getName() + " to " + afr.getCaptEQType());
			} else if (promoEQ.isEmpty())
				log.warn("No equipment program found for " + afr.getEquipmentType() + " in " + usrLoc.getDB());
			else
				log.info(afr.getEquipmentType() + " not in " + p.getEquipmentType() + " program " + eq.getPrimaryRatings());
			
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
			List<? extends GeospaceLocation> rtEntries = fddao.getRouteEntries(flightID, false, false);
			ETOPSResult etopsClass = ETOPSHelper.classify(rtEntries);
			afr.setAttribute(FlightReport.ATTR_ETOPSWARN, ETOPSHelper.validate(a, etopsClass.getResult()));
			if (afr.hasAttribute(FlightReport.ATTR_ETOPSWARN))
				comments.add("SYSTEM: ETOPS classificataion " + String.valueOf(etopsClass));
			
			// Check prohibited airspace
			Collection<Airspace> rstAirspaces = AirspaceHelper.classify(rtEntries, true); 
			if (!rstAirspaces.isEmpty()) {
				afr.setAttribute(FlightReport.ATTR_AIRSPACEWARN, true);
				comments.add("SYSTEM: Entered restricted airspace " + StringUtils.listConcat(rstAirspaces, ", "));
			}
			
			// Calculate gates
			Gate gD = null; Gate gA = null;
			if (rtEntries.size() > 1) {
				ctx.setMessage("Calculating departure/arrival Gates");
				GeoComparator dgc = new GeoComparator(rtEntries.get(0), true);
				GeoComparator agc = new GeoComparator(rtEntries.get(rtEntries.size() - 1), true);
			
				// Get the closest departure gate
				GetGates gdao = new GetGates(con);
				SortedSet<Gate> dGates = new TreeSet<Gate>(dgc);
				dGates.addAll(gdao.getAllGates(afr.getAirportD(), info.getSimulator()));
				gD = dGates.isEmpty() ? null : dGates.first();
				
				// Get the closest arrival gate
				SortedSet<Gate> aGates = new TreeSet<Gate>(agc);
				aGates.addAll(gdao.getAllGates(afr.getAirportA(), info.getSimulator()));
				gA = aGates.isEmpty() ? null : aGates.first();
			}
			
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
			if (!avgHours.hasCurrent() && !avgHours.hasHistoric() && !isAcademy && !isAssignment && !isEvent) {
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

			// Load the departure runway
			GetNavAirway navdao = new GetNavAirway(con);
			Runway rD = null;
			LandingRunways lr = navdao.getBestRunway(info.getAirportD(), afr.getSimulator(), afr.getTakeoffLocation(), afr.getTakeoffHeading());
			Runway r = lr.getBestRunway();
			if (r != null) {
				int dist = GeoUtils.distanceFeet(r, afr.getTakeoffLocation());
				rD = new RunwayDistance(r, dist);
				if (r.getLength() < a.getTakeoffRunwayLength()) {
					comments.add("SYSTEM: Minimum takeoff runway length for the " + a.getName() + " is " + a.getTakeoffRunwayLength() + " feet");
					afr.setAttribute(FlightReport.ATTR_RWYWARN, true);
				}
				if (!r.getSurface().isHard() && !a.getUseSoftRunways()) {
					comments.add("SYSTEM: " + a.getName() + " not authorized for soft runway operation on " + r.getName());
					afr.setAttribute(FlightReport.ATTR_RWYSFCWARN, true);
				}
			}

			// Load the arrival runway
			Runway rA = null;
			lr = navdao.getBestRunway(afr.getAirportA(), afr.getSimulator(), afr.getLandingLocation(), afr.getLandingHeading());
			r = lr.getBestRunway();
			if (r != null) {
				int dist = GeoUtils.distanceFeet(r, afr.getLandingLocation());
				rA = new RunwayDistance(r, dist);
				if (r.getLength() < a.getLandingRunwayLength()) {
					comments.add("SYSTEM: Minimum landing runway length for the " + a.getName() + " is " + a.getLandingRunwayLength() + " feet");
					afr.setAttribute(FlightReport.ATTR_RWYWARN, true);
				}
				if (!r.getSurface().isHard() && !a.getUseSoftRunways()) {
					comments.add("SYSTEM: " + a.getName() + " not authorized for soft runway operation on " + r.getName());
					afr.setAttribute(FlightReport.ATTR_RWYSFCWARN, true);
				}
			}
			
			// Get framerates
			afr.setAverageFrameRate(fddao.getFrameRate(flightID));
			
			// Set misc options
			afr.setClientBuild(ac.getClientBuild());
			afr.setBeta(ac.getBeta());
			afr.setAttribute(FlightReport.ATTR_DISPATCH, info.isDispatchPlan());
			if (afr.getDatabaseID(DatabaseID.ACARS) == 0)
				afr.setDatabaseID(DatabaseID.ACARS, flightID);

			// Start the transaction
			String promoLegs = afr.getCaptEQType().toString();
			ctx.startTX();

			// Mark the PIREP as filed
			SetInfo idao = new SetInfo(con);
			idao.logPIREP(flightID);
			info.setComplete(true);

			// Clean up the memcached track data
			SetTrack tkwdao = new SetTrack();
			tkwdao.clear(flightID);
			
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
					cr.setSubmittedOn(Instant.now());
					cr.setStatus(TestStatus.SUBMITTED);
					if (cr.getAcademy() && !afr.hasAttribute(FlightReport.ATTR_ACADEMY))
						afr.setAttribute(FlightReport.ATTR_ACADEMY, true);

					// Update the checkride
					SetExam wdao = new SetExam(con);
					wdao.write(cr);
				} else
					afr.setAttribute(FlightReport.ATTR_CHECKRIDE, false);
			}

			// Write the runway/gate data
			SetACARSRunway awdao = new SetACARSRunway(con);
			awdao.writeRunways(flightID, rD, rA);
			awdao.writeGates(flightID, gD, gA);
			
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
				
				// Check what SID we filed
				String trName = wps.get(0); String trTrans = wps.get(1);
				if (!StringUtils.isEmpty(info.getSID())) {
					TerminalRoute fSID = navdao.getRoute(afr.getAirportD(), TerminalRoute.Type.SID, info.getSID(), true);
					if (fSID != null) {
						trName = fSID.getName(); 
						trTrans = fSID.getTransition();
					}
				}

				// Check actual SID
				TerminalRoute aSID = navdao.getBestRoute(afr.getAirportD(), TerminalRoute.Type.SID, trName, trTrans, rD);
				if ((aSID != null) && (!aSID.getCode().equals(info.getSID()))) {
					awdao.clearSID(flightID);
					awdao.writeSIDSTAR(flightID, aSID);
					if ((ac.getVersion() > 2) || p.isInRole("Developer"))
						comments.add("SYSTEM: Filed SID was " + info.getSID() + ", actual was " + aSID.getCode());
				}
				
				// Check what STAR we filed
				trName = wps.get(wps.size() - 1); trTrans = wps.get(wps.size() - 2);
				if (!StringUtils.isEmpty(info.getSTAR())) {
					TerminalRoute fSTAR = navdao.getRoute(afr.getAirportA(), TerminalRoute.Type.STAR, info.getSTAR(), true);
					if (fSTAR != null) {
						trName = fSTAR.getName();
						trTrans = fSTAR.getTransition();
					}
				}

				// Check actual STAR
				TerminalRoute aSTAR = navdao.getBestRoute(afr.getAirportA(), TerminalRoute.Type.STAR, trName, trTrans, rA);
				if (aSTAR == null)
					aSTAR = navdao.getBestRoute(afr.getAirportA(), TerminalRoute.Type.STAR, trName, null, rA); 
				if ((aSTAR != null) && (!aSTAR.getCode().equals(info.getSTAR()))) {
					awdao.clearSTAR(flightID);
					awdao.writeSIDSTAR(flightID, aSTAR);
					if ((ac.getVersion() > 2) || p.isInRole("Developer"))
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
			
			// FIXME: Check promoEQ stayed the same
			String promoLegs2 = afr.getCaptEQType().toString();
			if (!promoLegs.equals(promoLegs2))
				log.warn("PromotionEQ was " + promoLegs + ", now " + promoLegs2);

			// Save the PIREP ID in the ACK message and send the ACK
			ackMsg.setEntry("pirepID", afr.getHexID());
			ackMsg.setEntry("protocol", usrAirline.getSSL() ? "https" : "http");
			ackMsg.setEntry("domain", usrLoc.getDomain());
			ctx.push(ackMsg, ac.getID(), true);
			
			// Send a notification message if a check ride, either to the CP/ACP, or the Instructor(s)
			GetMessageTemplate mtdao = new GetMessageTemplate(con);
			if ((cr != null) && afr.hasAttribute(FlightReport.ATTR_ACADEMY)) {
				ctx.setMessage("Sending Flight Academy check ride notification");
				MessageContext mctxt = new MessageContext(usrLoc.getAirlineCode());
				mctxt.addData("user", p);
				mctxt.addData("pirep", afr);
				mctxt.addData("airline", usrAirline.getName());
				mctxt.addData("url", (usrAirline.getSSL() ? "http" : "https") + "://www." + usrLoc.getDomain() + "/");
					
				// Load the template
				mctxt.setTemplate(mtdao.get(usrLoc.getDB(), "CRSUBMIT"));
					
				// Get the Instructor(s)
				GetUserData uddao = new GetUserData(con);
				Collection<Pilot> insList = new TreeSet<Pilot>();
				if ((c != null) && (c.getInstructorID() != 0)) {
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
			} else if (afr.hasAttribute(FlightReport.ATTR_CHECKRIDE) && (cr != null)) {
				ctx.setMessage("Sending check ride notification");
				EquipmentType crEQ = eqdao.get(cr.getEquipmentType(), cr.getOwner().getDB());
				if (crEQ != null) {
					MessageContext mctxt = new MessageContext(crEQ.getOwner().getCode());
					mctxt.addData("user", p);
					mctxt.addData("pirep", afr);
					mctxt.addData("airline", crEQ.getOwner().getName());
					mctxt.addData("url", (eq.getOwner().getSSL() ? "http" : "https") + "://www." + eq.getOwner().getDomain() + "/");
					
					// Load the template
					mctxt.setTemplate(mtdao.get(crEQ.getOwner().getDB(), "CRSUBMIT"));

					// Load the equipment type ACPs
					Collection<Pilot> eqACPs = pdao.getPilotsByEQ(crEQ, null, true, Rank.ACP);

					// Send the message to the CP
					Mailer mailer = new Mailer(p);
					mailer.setContext(mctxt);
					for (Pilot acp : eqACPs)
						mailer.setCC(acp);
					
					mailer.send(pdao.getPilotsByEQ(crEQ, null, true, Rank.CP));
				}
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
	@Override
	public final int getMaxExecTime() {
		return 2500;
	}
}