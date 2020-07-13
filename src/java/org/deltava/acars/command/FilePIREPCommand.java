// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2014, 2015, 2106, 2017, 2018, 2019, 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;
import java.time.*;
import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.beans.*;
import org.deltava.beans.academy.*;
import org.deltava.beans.acars.*;
import org.deltava.beans.econ.*;
import org.deltava.beans.event.*;
import org.deltava.beans.flight.*;
import org.deltava.beans.navdata.*;
import org.deltava.beans.testing.*;
import org.deltava.beans.schedule.*;
import org.deltava.beans.servinfo.PositionData;
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
 * @version 9.0
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
		if (!ac.getUserID().equals(env.getOwnerID()))
			log.warn("Connection owned by " + ac.getUserID() + " Envelope owned by " + env.getOwnerID());

		// Get the PIREP data and flight information
		ACARSFlightReport afr = msg.getPIREP();
		InfoMessage info = ac.getFlightInfo();
		UserData usrLoc = ac.getUserData();
		AirlineInformation usrAirline = SystemData.getApp(usrLoc.getAirlineCode());
		afr.addStatusUpdate(usrLoc.getID(), HistoryType.LIFECYCLE, "Submitted via ACARS server");

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
			ctx.push(ackMsg);
			return;
		}

		int flightID = info.getFlightID();
		Connection con = null;
		try {
			con = ctx.getConnection();
			
			// Flush the position queue
			ctx.setMessage("Flushing Position Queue");
			int flushed = flush(true, ctx);
			if (flushed > 0)
				log.info("Flushed " + flushed + " Position records from queue");

			// Check for existing PIREP with this flight ID
			ctx.setMessage("Checking for duplicate Flight Report from " + ac.getUserID());
			GetFlightReportACARS prdao = new GetFlightReportACARS(con);
			GetPositionCount pcdao = new GetPositionCount(con);
			if (flightID != 0) {
				FDRFlightReport afr2 = prdao.getACARS(usrLoc.getDB(), flightID);
				if (afr2 != null) {
					ctx.release();	

					// Save the PIREP ID in the ACK message
					ackMsg.setEntry("domain", usrLoc.getDomain());
					ackMsg.setEntry("pirepID", afr2.getHexID());
					
					// Log warning and return an ACK
					log.warn("Flight " + flightID + " already has PIREP");
					ctx.push(ackMsg);
					return;
				}
				
				// Check for flight ID filed around the same time
				List<PositionCount> cnts = pcdao.getDuplicateID(flightID);
				if (cnts.size() > 1) {
					log.warn(cnts.size() + " Duplicate Flight records found for Flight " + flightID);
					cnts.forEach(pc -> log.warn("Flight " + pc.getID() + " = " + pc.getPositionCount() + " records"));
					int dupeID = cnts.get(0).getID(); 
					afr2 = prdao.getACARS(usrLoc.getDB(), dupeID);
					
					// If it has a PIREP, ACK with that PIREP's ID. If it doesn't, use that flight ID for this PIREP.
					if (afr2 != null) {
						ctx.release();
						
						// Save the PIREP ID in the ACK message
						ackMsg.setEntry("domain", usrLoc.getDomain());
						ackMsg.setEntry("pirepID", afr2.getHexID());

						// Log warning and return an ACK
						log.warn("Ignoring duplicate PIREP submission from " + ac.getUserID() + ", FlightID = " + afr2.getDatabaseID(DatabaseID.ACARS));
						ctx.push(ackMsg);
						return;
					}
					
					// If the flight ID with the most records is different, use it
					if (dupeID != flightID) {
						log.warn(dupeID + " has more positions, switching Flight ID from " + flightID);
						afr.setDatabaseID(DatabaseID.ACARS, dupeID);
						flightID = dupeID;
					}
				}
			} else {
				List<FlightReport> dupes = prdao.checkDupes(usrLoc.getDB(), afr, usrLoc.getID());
				dupes.addAll(prdao.checkDupes(usrLoc.getDB(), flightID));
				if (dupes.size() > 0) {
					ctx.release();

					// Log warning and return an ACK
					log.warn("Ignoring possible duplicate PIREP from " + ac.getUserID());
					ctx.push(ackMsg);
					return;
				}
			}

			// Log number of positions
			int positionCount = pcdao.getCount(flightID).getPositionCount();
			if (positionCount == 0)
				log.warn("No position records for Flight " + info.getFlightID());
			
			// If we found a draft flight report, save its database ID and copy its ID to the PIREP we will file
			ctx.setMessage("Checking for draft Flight Reports by " + ac.getUserID());
			List<FlightReport> dFlights = prdao.getDraftReports(usrLoc.getID(), afr, usrLoc.getDB());
			if (!dFlights.isEmpty()) {
				FlightReport fr = dFlights.get(0);
				afr.setID(fr.getID());
				afr.setDatabaseID(DatabaseID.ASSIGN, fr.getDatabaseID(DatabaseID.ASSIGN));
				afr.setDatabaseID(DatabaseID.EVENT, fr.getDatabaseID(DatabaseID.EVENT));
				afr.setAttribute(FlightReport.ATTR_CHARTER, fr.hasAttribute(FlightReport.ATTR_CHARTER));
				afr.setAttribute(FlightReport.ATTR_DIVERT, fr.hasAttribute(FlightReport.ATTR_DIVERT));
				if (!StringUtils.isEmpty(fr.getComments()))
					afr.setComments(fr.getComments());
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
			int dayOfYear = ZonedDateTime.now().getDayOfYear();
			ZonedDateTime zdt = ZonedDateTime.ofInstant(afr.getDate(), p.getTZ().getZone());
			if (zdt.getDayOfYear() != dayOfYear) {
				afr.addStatusUpdate(0, HistoryType.SYSTEM, "Adjusted date to " + StringUtils.format(zdt, "MM/dd/yyyy") + ", Pilot in " + p.getTZ().toString());
				afr.setDate(zdt.toInstant());
			}

			// Check that the user has an online network ID
			OnlineNetwork network = afr.getNetwork();
			if ((network != null) && (!p.hasNetworkID(network))) {
				log.info(p.getName() + " does not have a " + network.toString() + " ID");
				afr.addStatusUpdate(0, HistoryType.SYSTEM, "No " + network.toString() + " ID, resetting Online Network flag");
				afr.setNetwork(null);
				afr.setDatabaseID(DatabaseID.EVENT, 0);
			} else if ((network == null) && (afr.getDatabaseID(DatabaseID.EVENT) != 0)) {
				afr.addStatusUpdate(0, HistoryType.SYSTEM, "Filed offline, resetting Online Event flag");
				afr.setDatabaseID(DatabaseID.EVENT, 0);
			}
			
			// Load track data
			Collection<PositionData> pd = new ArrayList<PositionData>(); int trackID = 0;
			if (afr.hasAttribute(FlightReport.ATTR_ONLINE_MASK)) {
				GetOnlineTrack tdao = new GetOnlineTrack(con); 
				trackID = tdao.getTrackID(afr.getDatabaseID(DatabaseID.PILOT), afr.getNetwork(), afr.getSubmittedOn(), afr.getAirportD(), afr.getAirportA());
				if (trackID != 0) {
					pd.addAll(tdao.getRaw(trackID));
					afr.addStatusUpdate(0, HistoryType.SYSTEM, "Loaded " + afr.getNetwork() + " online track data (" + pd.size() + " positions)");	
				}
			}
			
			// Check if it's an Online Event flight
			GetEvent evdao = new GetEvent(con);
			EventFlightHelper efr = new EventFlightHelper(afr);
			if ((afr.getDatabaseID(DatabaseID.EVENT) == 0) && (network != null)) {
				List<Event> events = evdao.getPossibleEvents(afr, usrLoc.getAirlineCode());
				events.removeIf(e -> !efr.matches(e));
				if (!events.isEmpty()) {
					Event e = events.get(0);
					afr.addStatusUpdate(0, HistoryType.SYSTEM, "Detected participation in " + e.getName() + " Online Event");
					afr.setDatabaseID(DatabaseID.EVENT, e.getID());
				}
			}

			// Check that it was submitted in time
			Event e = evdao.get(afr.getDatabaseID(DatabaseID.EVENT));
			if ((e != null) && !efr.matches(e)) {
				afr.addStatusUpdate(0, HistoryType.SYSTEM, efr.getMessage());
				afr.setDatabaseID(DatabaseID.EVENT, 0);
			} else if ((e == null) && (afr.getDatabaseID(DatabaseID.EVENT) != 0)) {
				afr.addStatusUpdate(0, HistoryType.SYSTEM, "Unknown Online Event - " + afr.getDatabaseID(DatabaseID.EVENT));
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
						afr.addStatusUpdate(0, HistoryType.SYSTEM, "Not eligible for promotion: " + helper.getLastComment());
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

			// Check for excessive distance and diversion
			AircraftPolicyOptions opts = a.getOptions(usrLoc.getAirlineCode());
			afr.setAttribute(FlightReport.ATTR_RANGEWARN, (afr.getDistance() > opts.getRange()));
			afr.setAttribute(FlightReport.ATTR_DIVERT, afr.hasAttribute(FlightReport.ATTR_DIVERT) || !afr.getAirportA().equals(info.getAirportA()));

			// Check for excessive weight
			if ((a.getMaxTakeoffWeight() != 0) && (afr.getTakeoffWeight() > a.getMaxTakeoffWeight()))
				afr.setAttribute(FlightReport.ATTR_WEIGHTWARN, true);
			else if ((a.getMaxLandingWeight() != 0) && (afr.getLandingWeight() > a.getMaxLandingWeight()))
				afr.setAttribute(FlightReport.ATTR_WEIGHTWARN, true);
			
			// Check ETOPS
			GetACARSPositions fddao = new GetACARSPositions(con);
			List<? extends GeospaceLocation> rtEntries = fddao.getRouteEntries(flightID, false, false);
			ETOPSResult etopsClass = ETOPSHelper.classify(rtEntries);
			afr.setAttribute(FlightReport.ATTR_ETOPSWARN, ETOPSHelper.validate(a.getOptions(usrLoc.getAirlineCode()), etopsClass.getResult()));
			if (afr.hasAttribute(FlightReport.ATTR_ETOPSWARN))
				afr.addStatusUpdate(0, HistoryType.SYSTEM, "ETOPS classificataion " + String.valueOf(etopsClass));
			
			// Check prohibited airspace
			Collection<Airspace> rstAirspaces = AirspaceHelper.classify(rtEntries, false); 
			if (!rstAirspaces.isEmpty()) {
				afr.setAttribute(FlightReport.ATTR_AIRSPACEWARN, true);
				afr.addStatusUpdate(0, HistoryType.SYSTEM, "Entered restricted airspace " + StringUtils.listConcat(rstAirspaces, ", "));
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
				if (Math.abs(afr.getLoadFactor() - info.getLoadFactor()) > 0.01)
					afr.addStatusUpdate(0, HistoryType.SYSTEM, "Load factor mismatch for " + flightID + "! Flight = " + info.getLoadFactor() + ", PIREP = " + afr.getLoadFactor());
				
				if ((afr.getLoadFactor() <= 0) && (info.getLoadFactor() <= 0)) {
					ctx.setMessage("Calculating flight load factor");
					LoadFactor lf = new LoadFactor((EconomyInfo) IPCUtils.reserialize(econ));
					double loadFactor = lf.generate(afr.getDate());
					log.info("Calculated load factor of " + loadFactor + ", was " + afr.getLoadFactor() + " for Flight " + flightID);
					afr.setLoadFactor(loadFactor);
				} else if (info.getLoadFactor() > 0) {
					afr.setLoadFactor(info.getLoadFactor());
					log.info("Using flight " + flightID + " data load factor of " + info.getLoadFactor());
				}
				
				if ((opts.getSeats() > 0) && (afr.getPassengers() == 0)) {
					int paxCount = (int) Math.round(opts.getSeats() * afr.getLoadFactor());
					afr.setPassengers(Math.min(opts.getSeats(), paxCount));
					if (paxCount > opts.getSeats())
						afr.addStatusUpdate(0, HistoryType.SYSTEM, "Invalid passenger count - pax=" + paxCount + ", seats=" + opts.getSeats());
				}
			}
			
			// Check for in-flight refueling
			ctx.setMessage("Checking for In-Flight Refueling");
			GetRefuelCheck rfdao = new GetRefuelCheck(con);
			FuelUse use = FuelUse.validate(rfdao.checkRefuel(flightID));
			afr.setTotalFuel(use.getTotalFuel());
			afr.setAttribute(FlightReport.ATTR_REFUELWARN, use.getRefuel());
			use.getMessages().forEach(fuelMsg -> afr.addStatusUpdate(0, HistoryType.SYSTEM, fuelMsg));
			
			// Check if it's a Flight Academy flight
			ctx.setMessage("Checking for Flight Academy flight");
			GetRawSchedule rsdao = new GetRawSchedule(con);
			GetScheduleSearch sdao = new GetScheduleSearch(con);
			sdao.setSources(rsdao.getSources(true, usrLoc.getDB()));
			ScheduleEntry sEntry = sdao.get(afr, usrLoc.getDB());
			boolean isAcademy = a.getAcademyOnly() || ((sEntry != null) && sEntry.getAcademy());
			
			// If we're an Academy flight, check if we have an active course
			Course c = null;
			if (isAcademy) {
				GetAcademyCourses crsdao = new GetAcademyCourses(con);
				Collection<Course> courses = crsdao.getByPilot(usrLoc.getID());
				c = courses.stream().filter(crs -> (crs.getStatus() == org.deltava.beans.academy.Status.STARTED)).findAny().orElse(null);
				boolean isINS = p.isInRole("Instructor") ||  p.isInRole("AcademyAdmin") || p.isInRole("AcademyAudit") || p.isInRole("Examiner");
				afr.setAttribute(FlightReport.ATTR_ACADEMY, (c != null) || isINS);	
				if (!afr.hasAttribute(FlightReport.ATTR_ACADEMY))
					afr.addStatusUpdate(0, HistoryType.SYSTEM, "Removed Flight Academy status - No active Course");
			}

			// Check the schedule database and check the route pair
			ctx.setMessage("Checking schedule for " + afr.getAirportD() + " to " + afr.getAirportA());
			boolean isAssignment = (afr.getDatabaseID(DatabaseID.ASSIGN) != 0);
			boolean isEvent = (afr.getDatabaseID(DatabaseID.EVENT) != 0);
			FlightTime avgHours = sdao.getFlightTime(afr, usrLoc.getDB()); ScheduleEntry onTimeEntry = null;
			if ((avgHours.getType() == RoutePairType.UNKNOWN) && !isAcademy && !isAssignment && !isEvent) {
				log.warn("No flights found between " + afr.getAirportD() + " and " + afr.getAirportA());
				boolean wasValid = info.isScheduleValidated() && info.matches(afr.getAirportD(), afr.getAirportA());
				if (!wasValid)
					afr.setAttribute(FlightReport.ATTR_ROUTEWARN, !afr.hasAttribute(FlightReport.ATTR_CHARTER));
			} else {
				int minHours = (int) ((avgHours.getFlightTime() * 0.75) - 5); // fixed 0.5 hour
				int maxHours = (int) ((avgHours.getFlightTime() * 1.15) + 5);
				if ((afr.getLength() < minHours) || (afr.getLength() > maxHours))
					afr.setAttribute(FlightReport.ATTR_TIMEWARN, true);
				
				// Calculate timeliness of flight
				if (!afr.hasAttribute(FlightReport.ATTR_DIVERT)) {
					ScheduleSearchCriteria ssc = new ScheduleSearchCriteria("TIME_D"); ssc.setDBName(usrLoc.getDB());
					ssc.setAirportD(afr.getAirportD()); ssc.setAirportA(afr.getAirportA());
					ssc.setExcludeHistoric(afr.getAirline().getHistoric() ? Inclusion.INCLUDE : Inclusion.EXCLUDE);
					OnTimeHelper oth = new OnTimeHelper(sdao.search(ssc));
					afr.setOnTime(oth.validate(afr));
					onTimeEntry = oth.getScheduleEntry();
				}
			}

			// Load held PIREP count
			ctx.setMessage("Checking Held Flight Reports for " + ac.getUserID());
			int heldPIREPs = prdao.getHeld(usrLoc.getID(), usrLoc.getDB());
			if (heldPIREPs >= SystemData.getInt("users.pirep.maxHeld", 5)) {
				afr.addStatusUpdate(0, HistoryType.SYSTEM, "Automatically Held due to " + heldPIREPs + " held Flight Reports");
				afr.setStatus(FlightStatus.HOLD);
			}

			// Load the departure runway
			GetNavAirway navdao = new GetNavAirway(con);
			Runway rD = null;
			LandingRunways lr = navdao.getBestRunway(info.getAirportD(), afr.getSimulator(), afr.getTakeoffLocation(), afr.getTakeoffHeading());
			Runway r = lr.getBestRunway();
			if (r != null) {
				int dist = r.distanceFeet(afr.getTakeoffLocation());
				rD = new RunwayDistance(r, dist);
				if (r.getLength() < opts.getTakeoffRunwayLength()) {
					afr.addStatusUpdate(0, HistoryType.SYSTEM, "Minimum takeoff runway length for the " + a.getName() + " is " + opts.getTakeoffRunwayLength() + " feet");
					afr.setAttribute(FlightReport.ATTR_RWYWARN, true);
				}
				if (!r.getSurface().isHard() && !opts.getUseSoftRunways()) {
					afr.addStatusUpdate(0, HistoryType.SYSTEM, a.getName() + " not authorized for soft runway operation on " + r.getName());
					afr.setAttribute(FlightReport.ATTR_RWYSFCWARN, true);
				}
			}

			// Load the arrival runway
			Runway rA = null;
			lr = navdao.getBestRunway(afr.getAirportA(), afr.getSimulator(), afr.getLandingLocation(), afr.getLandingHeading());
			r = lr.getBestRunway();
			if (r != null) {
				int dist = r.distanceFeet(afr.getLandingLocation());
				rA = new RunwayDistance(r, dist);
				if (r.getLength() < opts.getLandingRunwayLength()) {
					afr.addStatusUpdate(0, HistoryType.SYSTEM, "Minimum landing runway length for the " + a.getName() + " is " + opts.getLandingRunwayLength() + " feet");
					afr.setAttribute(FlightReport.ATTR_RWYWARN, true);
				}
				if (!r.getSurface().isHard() && !opts.getUseSoftRunways()) {
					afr.addStatusUpdate(0, HistoryType.SYSTEM, a.getName() + " not authorized for soft runway operation on " + r.getName());
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
			ctx.startTX();

			// Mark the PIREP as filed
			SetInfo idao = new SetInfo(con);
			idao.logPIREP(flightID);
			info.setComplete(true);

			// Clean up the memcached track data
			SetTrack tkwdao = new SetTrack();
			tkwdao.clear(true, String.valueOf(flightID));
			
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
				
				afr.setAttribute(FlightReport.ATTR_CHECKRIDE, (cr != null));
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
				}
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
					awdao.clearTerminalRoutes(flightID, TerminalRoute.Type.SID);
					awdao.writeSIDSTAR(flightID, aSID);
					if ((ac.getVersion() > 2) || p.isInRole("Developer"))
						afr.addStatusUpdate(0, HistoryType.SYSTEM, "Filed SID was " + info.getSID() + ", actual was " + aSID.getCode());
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
					awdao.clearTerminalRoutes(flightID, TerminalRoute.Type.STAR);
					awdao.writeSIDSTAR(flightID, aSTAR);
					if ((ac.getVersion() > 2) || p.isInRole("Developer"))
						afr.addStatusUpdate(0, HistoryType.SYSTEM, "Filed STAR was " + info.getSTAR() + ", actual was " + aSTAR.getCode());
				}
			}
			
			// Get the write DAO and save the PIREP
			ctx.setMessage("Saving Flight report for flight " + afr.getFlightCode() + " for " + ac.getUserID());
			SetFlightReport wdao = new SetFlightReport(con);
			wdao.write(afr, usrLoc.getDB());
			wdao.writeACARS(afr, usrLoc.getDB());
			if (wdao.updatePaxCount(afr.getID(), usrLoc))
				log.warn("Updated Passenger count for PIREP #" + afr.getID());
			
			// Write online track data
			if (!pd.isEmpty()) {
				SetOnlineTrack twdao = new SetOnlineTrack(con);
				twdao.write(afr.getID(), pd, usrLoc.getDB());
				twdao.purgeRaw(trackID);
			}
			
			// Write ontime data if there is any
			if (afr.getOnTime() != OnTime.UNKNOWN) {
				SetACARSOnTime aowdao = new SetACARSOnTime(con);
				aowdao.write(usrLoc.getDB(), afr, onTimeEntry);
				ackMsg.setEntry("onTime", afr.getOnTime().toString());
				if (onTimeEntry != null)
					ackMsg.setEntry("onTimeFlight", onTimeEntry.getFlightCode());
			}
			
			// Commit the transaction
			ctx.commitTX();
			
			// Save the PIREP ID in the ACK message and send the ACK
			ackMsg.setEntry("pirepID", afr.getHexID());
			ackMsg.setEntry("protocol", "https");
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
				mctxt.addData("url", "https://www." + usrLoc.getDomain() + "/");
					
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
				EMailAddress sender = MailUtils.makeAddress("acars", usrLoc.getDomain(), "ACARS");
				Mailer mailer = new Mailer(sender);
				mailer.setContext(mctxt);
				mailer.send(insList);
				log.info("Sending Academy Check Ride notification to " + insList);
			} else if (afr.hasAttribute(FlightReport.ATTR_CHECKRIDE) && (cr != null)) {
				ctx.setMessage("Sending check ride notification");
				EquipmentType crEQ = eqdao.get(cr.getEquipmentType(), cr.getOwner().getDB());
				if (crEQ != null) {
					MessageContext mctxt = new MessageContext(crEQ.getOwner().getCode());
					mctxt.addData("user", p);
					mctxt.addData("pirep", afr);
					mctxt.addData("airline", crEQ.getOwner().getName());
					mctxt.addData("url", "https://www." + eq.getOwner().getDomain() + "/");
					
					// Load the template
					mctxt.setTemplate(mtdao.get(crEQ.getOwner().getDB(), "CRSUBMIT"));

					// Load the equipment type CP/ACPs
					Collection<Pilot> eqCPs = pdao.getPilotsByEQ(crEQ, null, true, Rank.ACP);
					eqCPs.addAll(pdao.getPilotsByEQ(crEQ, null, true, Rank.CP));

					// Send the message to the CP
					EMailAddress sender = MailUtils.makeAddress("acars", usrLoc.getDomain(), "ACARS");
					Mailer mailer = new Mailer(sender);
					mailer.setContext(mctxt);
					mailer.send(eqCPs);
					log.info("Sending Check Ride notification to " + eqCPs);
				}
			}
			
			log.info("PIREP " + afr.getID() + " from " + env.getOwner().getName() + " (" + env.getOwnerID() + ") filed");
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