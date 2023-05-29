// Copyright 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2014, 2016, 2017, 2019, 2020, 2021, 2022, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;
import java.time.*;
import java.sql.Connection;

import org.apache.logging.log4j.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;
import org.deltava.acars.util.SquawkGenerator;

import org.deltava.beans.*;
import org.deltava.beans.acars.*;
import org.deltava.beans.flight.*;
import org.deltava.beans.navdata.TerminalRoute;
import org.deltava.beans.schedule.*;
import org.deltava.beans.stats.*;
import org.deltava.beans.testing.*;

import org.deltava.dao.*;
import org.deltava.dao.acars.*;

import org.deltava.util.*;

/**
 * An ACARS Command to log Flight data.
 * @author Luke
 * @version 11.0
 * @since 1.0
 */

public class InfoCommand extends ACARSCommand {

	private static final Logger log = LogManager.getLogger(InfoCommand.class);

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
			log.warn(String.format("Connection owned by %s, Envelope owned by %s", con.getUserID(), env.getOwnerID()));

		// Build the acknowledge message
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());

		// Check for a duplicate Flight ID request
		if (assignID && (curInfo != null) && (curInfo.getFlightID() != 0) && !curInfo.isComplete()) {
			log.warn(String.format("Duplicate Flight ID request from %s - assigning Flight ID %d", con.getUserID(), Integer.valueOf(curInfo.getFlightID())));
			ackMsg.setEntry("flight_id", String.valueOf(curInfo.getFlightID()));
			ackMsg.setEntry("dispatchLogID", String.valueOf(curInfo.getDispatchLogID()));
			ackMsg.setEntry("tx", String.valueOf(curInfo.getTX()));
			ctx.push(ackMsg);
			return;
		}

		// Write the info to the database
		int oldID = msg.getFlightID();
		try {
			Connection c = ctx.getConnection();
			
			// If we're not requesting an ID, check that we don't have a dupe
			if (assignID) {
				GetPositionCount pcdao = new GetPositionCount(c);
				List<PositionCount> flightIDs = pcdao.find(usrLoc.getID(), msg.getStartTime());
				if (flightIDs.size() > 0) {
					log.warn(String.format("%s requesting new Flight ID", con.getUserID()));
					flightIDs.forEach(pc -> log.warn(String.format("Flight %d = %d records", Integer.valueOf(pc.getID()), Integer.valueOf(pc.getPositionCount()))));
					msg.setFlightID(flightIDs.get(0).getID());
					assignID = false; 
				}
			}

			// If we're requesting a specific ID, make sure we used to own it
			boolean isValidated = false;
			if (!assignID) {
				GetACARSData rdao = new GetACARSData(c);
				FlightInfo info = rdao.getInfo(msg.getFlightID()); 
				if (info == null) {
					log.warn(String.format("%s requesting invalid Flight %d", env.getOwnerID(), Integer.valueOf(msg.getFlightID())));
					assignID = true;
					msg.setFlightID(0);
				} else if (info.getAuthorID() != env.getOwner().getID()) {
					log.warn(String.format("%s requesting Flight %d - owned by ID %d", env.getOwnerID(), Integer.valueOf(msg.getFlightID()), Integer.valueOf(info.getAuthorID())));
					assignID = true;
					msg.setFlightID(0);
				} else if (info.getArchived() || info.getHasPIREP()) {
					log.warn(String.format("%d has PIREP or is archived!", Integer.valueOf(msg.getFlightID())));
					assignID = true;
					msg.setFlightID(0);
				} else {
					isValidated = info.isScheduleValidated();
					log.warn(String.format("%s revalidating Flight %d", msg.isServerRequsted() ? "Server" : "Client", Integer.valueOf(msg.getFlightID())));
					if ((msg.getDispatcher() != info.getDispatcher()) || (msg.getDispatcherID() != info.getDispatcherID())) {
						log.warn(String.format("Flight %d dispatcher was [%s/%d], now [%s/%d]", Integer.valueOf(msg.getFlightID()), info.getDispatcher(), Integer.valueOf(info.getDispatcherID()), msg.getDispatcher(), Integer.valueOf(msg.getDispatcherID())));
						if (msg.getDispatcherID() == 0) {
							msg.setDispatcher(info.getDispatcher());
							msg.setDispatcherID(info.getDispatcherID());
						}
					}
				}
			}
			
			// Initialize the schedule DAO
			GetRawSchedule rsdao = new GetRawSchedule(c);
			GetScheduleSearch sdao = new GetScheduleSearch(c);
			sdao.setSources(rsdao.getSources(true, usrLoc.getDB()));
			
			// Validate against the schedule - do this even if the message claims it's valid
			if (!isValidated) {
				RoutePair rt = RoutePair.of(msg.getAirportD(), msg.getAirportA());
				FlightTime avgTime = sdao.getFlightTime(rt, usrLoc.getDB());
				msg.setScheduleValidated(avgTime.getType() != RoutePairType.UNKNOWN);
				
				// If we're not valid, check against draft PIREPs
				GetFlightReports prdao = new GetFlightReports(c);
				if (!msg.isScheduleValidated()) {
					Collection<FlightReport> pireps = prdao.getDraftReports(usrLoc.getID(), msg, usrLoc.getDB());
					Optional<FlightReport> odfr = pireps.stream().filter(fr -> (fr.hasAttribute(FlightReport.ATTR_CHARTER) || (fr.getDatabaseID(DatabaseID.ASSIGN) > 0))).findAny();
					msg.setScheduleValidated(odfr.isPresent());
				}
				
				// If we're still not valid, check for an event
				if (!msg.isScheduleValidated()) {
					GetEvent edao = new GetEvent(c);
					boolean isOK = !edao.getPossibleEvents(msg, OnlineNetwork.VATSIM, msg.getStartTime(), usrLoc.getAirlineCode()).isEmpty();
					isOK |= !edao.getPossibleEvents(msg, OnlineNetwork.IVAO, msg.getStartTime(), usrLoc.getAirlineCode()).isEmpty();
					isOK |= !edao.getPossibleEvents(msg, OnlineNetwork.PILOTEDGE, msg.getStartTime(), usrLoc.getAirlineCode()).isEmpty();
					msg.setScheduleValidated(isOK);
				}
				
				// Check Tours
				if (!msg.isScheduleValidated()) {
					GetTour trdao = new GetTour(c);
					Collection<Tour> possibleTours = trdao.findLeg(msg, null, usrLoc.getDB());
					Instant minDate = Instant.ofEpochMilli(possibleTours.stream().mapToLong(t -> t.getStartDate().toEpochMilli()).min().orElse(msg.getStartTime().toEpochMilli()));
					Duration d = Duration.between(minDate, msg.getStartTime());
					Collection<FlightReport> oldPireps = prdao.getLogbookCalendar(usrLoc.getID(), usrLoc.getDB(), minDate, (int)d.toDaysPart() + 1);
					
					// Init the helper
					TourFlightHelper tfh = new TourFlightHelper(msg, true);
					tfh.addFlights(oldPireps);
					possibleTours.removeIf(t -> (tfh.isLeg(t) < 1));
					log.info(String.format("Possible Tours found - %s", possibleTours));
					if (!possibleTours.isEmpty()) {
						msg.setScheduleValidated(true);
						ackMsg.setEntry("possibleTour", "true");
						ackMsg.setEntry("tours", String.valueOf(possibleTours.size()));
					}
				}
			} else
				msg.setScheduleValidated(true);
			
			// Look for a check ride record - Builds prior to 103 send no check ride flag, but submit on PIREP
			if (!msg.isNoRideCheck()) {
				GetExam exdao = new GetExam(c);
				CheckRide cr = exdao.getCheckRide(usrLoc.getID(), msg.getEquipmentType(), TestStatus.NEW);
				ackMsg.setEntry("checkRide", String.valueOf(cr != null));
			} else if (msg.isCheckRide())
				ackMsg.setEntry("checkRide", "true");
			
			// Check for on-time data
			if (msg.isScheduleValidated() && (msg.getSimStartTime() != null) && !msg.isCheckRide()) {
				ScheduleSearchCriteria ssc = new ScheduleSearchCriteria("TIME_D"); ssc.setDBName(usrLoc.getDB());
				ssc.setAirportD(msg.getAirportD()); ssc.setAirportA(msg.getAirportA());
				ssc.setExcludeHistoric(!msg.getAirline().getHistoric() ? Inclusion.EXCLUDE : Inclusion.INCLUDE);
				OnTimeHelper oth = new OnTimeHelper(sdao.search(ssc));
				ackMsg.setEntry("onTime", String.valueOf(oth.validateDeparture(msg)));
				if (oth.getScheduleEntry() != null) {
					GetACARSOnTime otdao = new GetACARSOnTime(c);
					OnTimeStatsEntry otStats = otdao.getOnTimeStatistics(msg, usrLoc.getDB());
					ackMsg.setEntry("onTimeLegs", String.valueOf(otStats.getOnTimeLegs()));
					ackMsg.setEntry("onTimeTotal", String.valueOf(otStats.getTotalLegs()));
					
					ScheduleEntry se = oth.getScheduleEntry();
					ackMsg.setEntry("onTimeFlight", se.getFlightCode());
					ackMsg.setEntry("onTimeLeg", String.valueOf(se.getLeg()));
					ackMsg.setEntry("onTimeDeparture", StringUtils.format(se.getTimeD(), "HH:mm"));
					ackMsg.setEntry("onTimeArrival", StringUtils.format(se.getTimeA(), "HH:mm"));
				}
			}
			
			// Load passenger count for 156+ that submits pax and seats
			GetAircraft acdao = new GetAircraft(c);
			Aircraft ac = acdao.get(msg.getEquipmentType());
			if (ac != null) {
				msg.setEngineCount(ac.getEngines());
				AircraftPolicyOptions opts = ac.getOptions(usrLoc.getAirlineCode());
				if (msg.getSeats() == 0) msg.setSeats(opts.getSeats());
			} else {
				log.warn(String.format("Unknown aircraft type - %s", msg.getEquipmentType()));
				msg.setEngineCount(2);
			}
				
			// Get the SID/STAR data
			GetNavAirway navdao = new GetNavAirway(c);
			TerminalRoute sid = navdao.getRoute(msg.getAirportD(), TerminalRoute.Type.SID, msg.getSID(), true);
			TerminalRoute star = navdao.getRoute(msg.getAirportA(), TerminalRoute.Type.STAR, msg.getSTAR(), true);
			
			// Log unknown SID/STAR
			if ((sid == null) && (!StringUtils.isEmpty(msg.getSID())))
				log.warn(String.format("Unknown SID - %s",msg.getSID()));
			else if ((sid != null) && !sid.getCode().equals(msg.getSID()))
				msg.setSID(sid.getCode());
			if ((star == null) && (!StringUtils.isEmpty(msg.getSTAR())))
				log.warn(String.format("Unknown STAR - %s", msg.getSTAR()));
			else if ((star != null) && !star.getCode().equals(msg.getSTAR()))
				msg.setSTAR(star.getCode());
			
			// Validate the dispatch route
			if (msg.getRouteID() != 0) {
				GetACARSRoute ardao = new GetACARSRoute(c);
				DispatchRoute rt = ardao.getRoute(msg.getRouteID());
				if (rt == null) {
					log.warn(String.format("Invalid Dispatch Route ID - %d", Integer.valueOf(msg.getRouteID())));
					msg.setRouteID(0);
				}
			}
			
			// Validate the dispatcher
			if (msg.getDispatcherID() != 0) {
				GetUserData uddao = new GetUserData(c);
				UserData ud = uddao.get(msg.getDispatcherID());
				if (ud == null) {
					log.warn(String.format("Invalid Dispatcher ID - %d", Integer.valueOf(msg.getDispatcherID())));
					msg.setDispatcherID(0);
				} else
					log.info(String.format("Validated Dispatcher %d for %s", Integer.valueOf(ud.getID()), env.getOwnerID()));
			}
			
			// Validate the dispatch log
			if (msg.getDispatchLogID() != 0) {
				GetACARSLog aldao = new GetACARSLog(c);
				DispatchLogEntry dle = aldao.getDispatchLog(msg.getDispatchLogID());
				if (dle == null) {
					log.warn(String.format("Invalid Dispatch Log ID - %d", Integer.valueOf(msg.getDispatchLogID())));
					msg.setDispatchLogID(0);
				} else
					log.info(String.format("Validated Dispatch Log %d for %s", Integer.valueOf(dle.getID()), env.getOwnerID()));
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
						log.warn(String.format("Squawk code %s already assigned", tx));
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
			
			// Link to dispatcher log
			if (msg.getDispatchLogID() != 0) {
				SetDispatch dlwdao = new SetDispatch(c);
				dlwdao.link(msg.getDispatchLogID(), msg.getFlightID());
			}
			
			ctx.commitTX();
		} catch (DAOException de) {
			ctx.rollbackTX();
			msg.setFlightID(oldID);
			log.error("Pilot = " + con.getUserID() + ", ID = " + oldID + ", RouteID = " + msg.getRouteID() + ", DispatcherID = " + msg.getDispatcherID());
			log.error(de.getMessage(), de);
		} finally {
			ctx.release();
		}

		// Log returned flight id
		if (assignID)
			log.info(String.format("Assigned Flight ID %d to %s", Integer.valueOf(msg.getFlightID()), env.getOwnerID()));
		else
			log.info(String.format("%s resuming Flight %d", env.getOwnerID(), Integer.valueOf(msg.getFlightID())));

		// Create the ack message and envelope - these are always acknowledged
		ackMsg.setEntry("flight_id", String.valueOf(msg.getFlightID()));
		ackMsg.setEntry("dispatchLogID", String.valueOf(msg.getDispatchLogID()));
		ackMsg.setEntry("tx", String.valueOf(msg.getTX()));		
		ackMsg.setEntry("schedValid", String.valueOf(msg.isScheduleValidated()));
		ctx.push(ackMsg, env.getConnectionID(), true);

		// Set the info for the connection
		con.setFlightInfo(msg);
	}
}