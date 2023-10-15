// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2014, 2015, 2106, 2017, 2018, 2019, 2020, 2021, 2022, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;
import java.time.*;
import java.sql.Connection;

import org.apache.logging.log4j.*;

import org.deltava.beans.*;
import org.deltava.beans.acars.*;
import org.deltava.beans.econ.*;
import org.deltava.beans.flight.*;
import org.deltava.beans.navdata.*;
import org.deltava.beans.testing.*;
import org.deltava.beans.system.AirlineInformation;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;
import org.deltava.acars.util.FlightInfoHelper;

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
 * @version 11.1
 * @since 1.0
 */

public class FilePIREPCommand extends PositionCacheCommand {

	private static final Logger log = LogManager.getLogger(FilePIREPCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Log PIREP filing
		log.info("Receiving PIREP from {} ({})", env.getOwner().getName(), env.getOwnerID());

		// Get the Message and the ACARS connection
		FlightReportMessage msg = (FlightReportMessage) env.getMessage();
		ACARSConnection ac = ctx.getACARSConnection();
		if ((ac == null) || ac.getIsDispatch())
			return;
		
		// Generate the response message
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(ac.getUser(), msg.getID());
		if (!ac.getUserID().equals(env.getOwnerID()))
			log.warn("Connection owned by {} Envelope owned by {}", ac.getUserID(), env.getOwnerID());

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
		if (ac.getTimeOffset() > 5000)
			afr.addStatusUpdate(0, HistoryType.UPDATE, String.format("Adjusted times by %d ms", Long.valueOf(ac.getTimeOffset())));
		
		afr.addStatusUpdate(usrLoc.getID(), HistoryType.LIFECYCLE, "Submitted via ACARS server");

		// If we have no flight info, then push it back
		if (info == null) {
			log.warn("No Flight Information for Connection {}", StringUtils.formatHex(ac.getID()));
			ackMsg.setEntry("sendInfo", "true");
			ctx.push(ackMsg);
			return;
		}

		// Ensure we are taking the Flight ID from the latest InfoMessage
		int flightID = info.getFlightID();
		if (flightID != afr.getDatabaseID(DatabaseID.ACARS)) {
			log.warn("Flight Report flightID = {} Connection flightID = {}", Integer.valueOf(afr.getDatabaseID(DatabaseID.ACARS)), Integer.valueOf(flightID));
			afr.setDatabaseID(DatabaseID.ACARS, flightID);
		}
		
		Connection con = null;
		try {
			con = ctx.getConnection();
			
			// Flush the position queue
			ctx.setMessage("Flushing Position Queue");
			int flushed = flush(true, ctx);
			if (flushed > 0)
				log.info("Flushed {} Position records from queue", Integer.valueOf(flushed));

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
					log.warn("Flight {} already has PIREP", Integer.valueOf(flightID));
					ctx.push(ackMsg);
					return;
				}
				
				// Check for flight ID filed around the same time
				List<PositionCount> cnts = pcdao.getDuplicateID(flightID);
				if (cnts.size() > 1) {
					log.warn("{} Duplicate Flight records found for Flight {}", Integer.valueOf(cnts.size()), Integer.valueOf(flightID));
					cnts.forEach(pc -> log.warn("Flight {} = {} records", Integer.valueOf(pc.getID()), Integer.valueOf(pc.getPositionCount())));
					int dupeID = cnts.get(0).getID(); 
					afr2 = prdao.getACARS(usrLoc.getDB(), dupeID);
					
					// If it has a PIREP, ACK with that PIREP's ID. If it doesn't, use that flight ID for this PIREP.
					if (afr2 != null) {
						ctx.release();
						
						// Save the PIREP ID in the ACK message
						ackMsg.setEntry("domain", usrLoc.getDomain());
						ackMsg.setEntry("pirepID", afr2.getHexID());

						// Log warning and return an ACK
						log.warn("Ignoring duplicate PIREP submission from {}, FlightID = {}", ac.getUserID(), Integer.valueOf(afr2.getDatabaseID(DatabaseID.ACARS)));
						ctx.push(ackMsg);
						return;
					}
					
					// If the flight ID with the most records is different, use it
					if (dupeID != flightID) {
						log.warn("{} has more positions, switching Flight ID from {}", Integer.valueOf(dupeID), Integer.valueOf(flightID));
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
					log.warn("Ignoring possible duplicate PIREP from {}", ac.getUserID());
					ctx.push(ackMsg);
					return;
				}
			}

			// Log number of positions
			int positionCount = pcdao.getCount(flightID).getPositionCount();
			if (positionCount == 0)
				log.warn("No position records for Flight {}", Integer.valueOf(info.getFlightID()));
			
			// Reload the User
			GetPilotDirectory pdao = new GetPilotDirectory(con);
			CacheManager.invalidate("Pilots", usrLoc.cacheKey());
			Pilot p = pdao.get(usrLoc);
			
			// Init the submission helper
			FlightSubmissionHelper fsh = new FlightSubmissionHelper(con, afr, p);
			fsh.setAirlineInfo(usrLoc.getAirlineCode(), usrLoc.getDB());
			fsh.setACARSInfo(FlightInfoHelper.convert(info));
			
			// If we found a draft flight report, save its database ID and copy its ID to the PIREP we will file
			ctx.setMessage("Checking for draft Flight Reports by " + ac.getUserID());
			fsh.checkFlightReports();

			// Add user data
			afr.setDatabaseID(DatabaseID.PILOT, p.getID());
			afr.setRank(p.getRank());
			afr.setSimulator(info.getSimulator());
			if (StringUtils.isEmpty(afr.getTailCode()))
				afr.setTailCode(info.getTailCode());

			// Convert the date into the user's local time zone
			LocalDate pd = ZonedDateTime.ofInstant(afr.getDate(), p.getTZ().getZone()).toLocalDate();
			LocalDate sd = LocalDate.now();
			Duration timeDelta = Duration.between(pd.atStartOfDay(), sd.atStartOfDay());
			if (sd.getDayOfYear() != pd.getDayOfYear()) {
				LocalDateTime pldt = LocalDateTime.of(pd, LocalTime.of(12, 0));
				afr.addStatusUpdate(0, HistoryType.SYSTEM, String.format("Adjusted date from %s to %s, Pilot in %s (-%d s)", StringUtils.format(sd, "MM/dd/yyyy"), StringUtils.format(pldt.atZone(ZoneOffset.UTC), "MM/dd/yyyy"), p.getTZ(), Long.valueOf(timeDelta.toSeconds())));
				afr.setDate(pldt.atZone(ZoneOffset.UTC).toInstant());
			}

			// Check Online status, and Online Event
			fsh.checkOnlineNetwork();
			fsh.checkOnlineEvent();

			// Load the aircraft
			ctx.setMessage("Checking type ratings for " + ac.getUserID());
			fsh.checkRatings();

			// Check ETOPS / Airspace /Gates
			GetACARSPositions fddao = new GetACARSPositions(con);
			fsh.addPositions(fddao.getRouteEntries(flightID, true, false));
			if (fsh.hasPositionData()) {
				ctx.setMessage("Validating Airspace");
				fsh.checkAirspace();
				ctx.setMessage("Calculating departure/arrival Gates");
				fsh.calculateGates();
			}
			
			// Check aircraft
			fsh.checkAircraft();
			
			// Calculate flight load factor if not set client-side
			java.io.Serializable econ = SharedData.get(SharedData.ECON_DATA + usrLoc.getAirlineCode());
			if (econ != null) {
				ctx.setMessage("Calculating flight load factor");
				if (afr.getPassengers() != info.getPassengers())
					afr.addStatusUpdate(0, HistoryType.SYSTEM, String.format("Passenger Load mismatch for %d! Flight = %d, PIREP = %d", Integer.valueOf(flightID), Integer.valueOf(info.getPassengers()), Integer.valueOf(afr.getPassengers())));
				
				fsh.calculateLoadFactor((EconomyInfo) IPCUtils.reserialize(econ));
			}
			
			// Check for in-flight refueling
			ctx.setMessage("Checking for In-Flight Refueling");
			if (fsh.hasPositionData())
				fsh.checkRefuel();
			
			// Check for tour
			ctx.setMessage("Checking Flight Tours for " + ac.getUserID());
			fsh.checkTour();
			
			// Check if it's a Flight Academy flight
			ctx.setMessage("Checking Flight Schedule");
			fsh.checkSchedule();
			
			// Load the runways
			ctx.setMessage("Calculating runways at " + afr.getAirportD() + " / " + afr.getAirportA());
			fsh.calculateRunways();
			
			// Set misc options
			afr.setAverageFrameRate(fddao.getFrameRate(flightID).getAverage());
			afr.setClientBuild(ac.getClientBuild());
			afr.setBeta(ac.getBeta());
			afr.setAttribute(FlightReport.ATTR_DISPATCH, (info.getDispatcher() == DispatchType.DISPATCH));
			afr.setAttribute(FlightReport.ATTR_SIMBRIEF, (info.getDispatcher() == DispatchType.SIMBRIEF));
			if (afr.getDatabaseID(DatabaseID.ACARS) == 0)
				afr.setDatabaseID(DatabaseID.ACARS, flightID);
			
			// Check for dispatch data
			if (msg.getDispatcher() != info.getDispatcher()) {
				afr.addStatusUpdate(0,  HistoryType.SYSTEM, String.format("Flight Dispatcher = %s, Message = %s", msg.getDispatcher(), info.getDispatcher()));
				log.warn("Flight Dispatcher = {}, Message = {} for Flight {}", msg.getDispatcher(), info.getDispatcher(), Integer.valueOf(info.getFlightID()));
			}
			
			// Start the transaction
			ctx.startTX();

			// Mark the PIREP as filed
			SetInfo idao = new SetInfo(con);
			idao.logPIREP(flightID);
			info.setComplete(true);
			
			// Check dispatch log
			if ((afr.getDatabaseID(DatabaseID.DISPATCH) != info.getDispatchLogID()) && (info.getDispatchLogID() == 0)) {
				GetACARSLog aldao = new GetACARSLog(con);
				DispatchLogEntry dle = aldao.getDispatchLog(afr.getDatabaseID(DatabaseID.DISPATCH));
				if (dle != null) {
					afr.addStatusUpdate(0, HistoryType.SYSTEM, "Setting ACARS Dispatch Log to " + afr.getDatabaseID(DatabaseID.DISPATCH));
					SetDispatch dlwdao = new SetDispatch(con);
					dlwdao.link(afr.getDatabaseID(DatabaseID.DISPATCH), info.getFlightID());	
				} else {
					afr.addStatusUpdate(0, HistoryType.SYSTEM, "Cannot find Dispatch Log " + afr.getDatabaseID(DatabaseID.DISPATCH));
					afr.setDatabaseID(DatabaseID.DISPATCH, 0);
				}
			}

			// Clean up the memcached track data
			SetTrack tkwdao = new SetTrack();
			tkwdao.clear(true, String.valueOf(flightID));
			
			// Update the checkride record (don't assume pilots check the box, because they don't)
			CheckRide cr = null;
			if (afr.hasAttribute(FlightReport.ATTR_CHECKRIDE)) {
				GetExam exdao = new GetExam(con);
				// Check for Academy chck ride
				if (fsh.getCourse() != null) {
					List<CheckRide> rides = exdao.getAcademyCheckRides(fsh.getCourse().getID(), TestStatus.NEW);
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
			FlightInfo inf = fsh.getACARSInfo();
			awdao.writeRunways(flightID, inf.getRunwayD(), inf.getRunwayA());
			awdao.writeGates(inf);
			
			// Check if we're a dispatch plan
			if ((msg.getDispatcher() == DispatchType.DISPATCH) && (info.getDispatcher() != DispatchType.DISPATCH)) {
				log.warn("Flight {} was not set as Dispatch, but PIREP has Dispatch flag!", Integer.valueOf(flightID));
				afr.setAttribute(FlightReport.ATTR_DISPATCH, true);
				
				// Validate the dispatch route ID
				GetACARSRoute ardao = new GetACARSRoute(con);
				DispatchRoute dr = ardao.getRoute(msg.getRouteID());
				if (dr == null) {
					log.warn("Invalid Dispatch Route - {}", Integer.valueOf(msg.getRouteID()));
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
				GetNavRoute navdao = new GetNavRoute(con);
				
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
				TerminalRoute aSID = navdao.getBestRoute(afr.getAirportD(), TerminalRoute.Type.SID, trName, trTrans, fsh.getACARSInfo().getRunwayD());
				if ((aSID != null) && (!aSID.getCode().equals(info.getSID()))) {
					awdao.clearTerminalRoutes(flightID, TerminalRoute.Type.SID);
					awdao.writeSIDSTAR(flightID, aSID);
					afr.addStatusUpdate(0, HistoryType.SYSTEM, String.format("Filed SID was %s, actual was %s", info.getSID(), aSID.getCode()));
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
				TerminalRoute aSTAR = navdao.getBestRoute(afr.getAirportA(), TerminalRoute.Type.STAR, trName, trTrans, fsh.getACARSInfo().getRunwayA());
				if (aSTAR == null)
					aSTAR = navdao.getBestRoute(afr.getAirportA(), TerminalRoute.Type.STAR, trName, null, fsh.getACARSInfo().getRunwayA()); 
				if ((aSTAR != null) && (!aSTAR.getCode().equals(info.getSTAR()))) {
					awdao.clearTerminalRoutes(flightID, TerminalRoute.Type.STAR);
					awdao.writeSIDSTAR(flightID, aSTAR);
					afr.addStatusUpdate(0, HistoryType.SYSTEM, String.format("Filed STAR was %s, actual was %s", info.getSTAR(), aSTAR.getCode()));
				}
			}
			
			// Get the write DAO and save the PIREP
			ctx.setMessage("Saving Flight report for flight " + afr.getFlightCode() + " for " + ac.getUserID());
			SetFlightReport wdao = new SetFlightReport(con);
			wdao.write(afr, usrLoc.getDB());
			wdao.writeACARS(afr, usrLoc.getDB());
			
			// Write online track data
			if (fsh.hasTrackData()) {
				SetOnlineTrack twdao = new SetOnlineTrack(con);
				twdao.write(afr.getID(), fsh.getTrackData(), usrLoc.getDB());
				twdao.purgeRaw(fsh.getTrackID());
			}
			
			// Write ontime data if there is any
			if (afr.getOnTime() != OnTime.UNKNOWN) {
				SetACARSOnTime aowdao = new SetACARSOnTime(con);
				aowdao.write(usrLoc.getDB(), afr, fsh.getOnTimeEntry());
				ackMsg.setEntry("onTime", afr.getOnTime().toString());
				if (fsh.getOnTimeEntry() != null)
					ackMsg.setEntry("onTimeFlight", fsh.getOnTimeEntry().getFlightCode());
			}
			
			// Commit the transaction
			ctx.commitTX();
			
			// Save the PIREP ID in the ACK message and send the ACK
			ackMsg.setEntry("pirepID", afr.getHexID());
			ackMsg.setEntry("flightID", Integer.toHexString(afr.getDatabaseID(DatabaseID.ACARS)));
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
				if ((fsh.getCourse() != null) && (fsh.getCourse().getInstructorID() != 0)) {
					Pilot ins = pdao.get(uddao.get(fsh.getCourse().getInstructorID()));
					if (ins != null)
						insList.add(ins);
				}
					
				if (insList.isEmpty())
					insList.addAll(pdao.getByRole("Instructor", usrLoc.getDB()));
					
				// Send the message to the Instructors
				EMailAddress sender = MailUtils.makeAddress("acars", usrLoc.getDomain(), "ACARS");
				Mailer mailer = new Mailer(sender);
				mailer.setContext(mctxt);
				mailer.send(insList);
				log.info("Sending Academy Check Ride notification to {}", insList);
			} else if (afr.hasAttribute(FlightReport.ATTR_CHECKRIDE) && (cr != null)) {
				ctx.setMessage("Sending check ride notification");
				GetEquipmentType eqdao = new GetEquipmentType(con);
				EquipmentType crEQ = eqdao.get(cr.getEquipmentType(), cr.getOwner().getDB());
				if (crEQ != null) {
					MessageContext mctxt = new MessageContext(crEQ.getOwner().getCode());
					mctxt.addData("user", p);
					mctxt.addData("pirep", afr);
					mctxt.addData("airline", crEQ.getOwner().getName());
					mctxt.addData("url", "https://www." + crEQ.getOwner().getDomain() + "/");
					
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
					log.info("Sending Check Ride notification to {}", eqCPs);
				}
			}
			
			log.info("PIREP {} [Flight {}] from {} ({}) filed", Integer.valueOf(afr.getID()), Integer.valueOf(afr.getDatabaseID(DatabaseID.ACARS)), env.getOwner().getName(), env.getOwnerID());
		} catch (DAOException de) {
			ctx.rollbackTX();
			log.atError().withThrowable(de).log("{} - {}", ac.getUserID(), de.getMessage());
			ackMsg.setEntry("error", "PIREP Submission failed - " + de.getMessage());
			ctx.push(ackMsg, ac.getID(), true);
		} finally {
			ctx.release();
		}
	}

	@Override
	public final int getMaxExecTime() {
		return 2500;
	}
}