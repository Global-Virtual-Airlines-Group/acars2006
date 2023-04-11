// Copyright 2008, 2009, 2011, 2012, 2014, 2015, 2017, 2019, 2020, 2021, 2022, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;
import java.sql.Connection;
import java.time.Instant;

import org.deltava.beans.*;
import org.deltava.beans.flight.*;
import org.deltava.beans.schedule.*;
import org.deltava.beans.stats.Tour;
import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;

import org.deltava.dao.*;

import org.deltava.util.GeoUtils;
import org.deltava.util.system.SystemData;

/**
 * An ACARS Command to validate that a route exists in the Flight Schedule or is part of a valid flight assignment, charter request or Flight Tour, and if any dispatch routes currently exist.
 * @author Luke
 * @version 10.6
 * @since 2.3
 */

public class FlightValidationCommand extends DataCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		AcknowledgeMessage rspMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
		UserData ud = ctx.getACARSConnection().getUserData();
		
		// Get the Airports
		Airport airportD = SystemData.getAirport(msg.getFlag("airportD"));
		Airport airportA = SystemData.getAirport(msg.getFlag("airportA"));
		if ((airportD == null) || (airportA == null)) {
			rspMsg.setEntry("error", "Unknown Airports");
			ctx.push(rspMsg);
			return;
		}
		
		// Create the route pair and do ETOPS classification
		ScheduleRoute rt = new ScheduleRoute(SystemData.getAirline(ud.getAirlineCode()), airportD, airportA);
		Collection<GeoLocation> gc = GeoUtils.greatCircle(airportD, airportA, 25);
		ETOPS re = ETOPSHelper.classify(gc).getResult();
		rspMsg.setEntry("etops", re.name());

		try {
			Connection con = ctx.getConnection();
			
			// Get the aircraft if provided, do ETOPS validation
			GetAircraft acdao = new GetAircraft(con);
			if (msg.hasFlag("eqType")) {
				Aircraft a = acdao.get(msg.getFlag("eqType"));
				if (a != null) {
					AircraftPolicyOptions opts = a.getOptions(ud.getAirlineCode());
					ETOPS ae = (opts == null) ? ETOPS.ETOPS90 : opts.getETOPS();
					rspMsg.setEntry("etopsWarn", String.valueOf(ETOPSHelper.isWarn(ae, re)));	
				} else
					log.warn(String.format("Unknown Aircraft - %s", msg.getFlag("eqType")));
			} else
				rspMsg.setEntry("etopsWarn", String.valueOf(ETOPSHelper.isWarn(ETOPS.ETOPS90, re)));

			// Check the route
			GetRawSchedule rsdao = new GetRawSchedule(con);
			GetSchedule sdao = new GetSchedule(con);
			sdao.setSources(rsdao.getSources(true, ud.getDB()));
			FlightTime flightTime = sdao.getFlightTime(rt, ud.getDB());
			boolean inSchedule = (flightTime.getFlightTime().toSeconds() > 0); boolean isValid = inSchedule;
			
			// Check for draft flights if not in schedule
			GetFlightReports frdao = new GetFlightReports(con);
			if (!inSchedule) {
				List<FlightReport> pireps = frdao.getDraftReports(ud.getID(), rt, ud.getDB());
				Optional<FlightReport> ofr = pireps.stream().filter(fr -> (fr.hasAttribute(FlightReport.ATTR_CHARTER) || (fr.getDatabaseID(DatabaseID.ASSIGN) != 0))).findAny();
				isValid = ofr.isPresent();
			}
			
			// Check Tours
			if (!isValid) {
				final Instant now = Instant.now();
				GetTour trdao = new GetTour(con);
				Collection<Tour> possibleTours = trdao.findLeg(RoutePair.of(airportD, airportA), null, ud.getDB());
				possibleTours.removeIf(t -> !t.isActiveOn(now));
				isValid |= !possibleTours.isEmpty();
				rspMsg.setEntry("possibleTour", String.valueOf(!possibleTours.isEmpty()));
			}
			
			rspMsg.setEntry("routeOK", String.valueOf(isValid));
			rspMsg.setEntry("hasCurrent", String.valueOf(flightTime.getType().hasCurrent()));
			rspMsg.setEntry("hasHistoric", String.valueOf(flightTime.getType().hasHistoric()));
			
			// Check for dispatch routes
			if (inSchedule) {
				GetACARSRoute drdao = new GetACARSRoute(con);
				int dRoutes = drdao.getRoutes(rt, true).size();
			
				// If we have no dispatch routes, check for cached routes
				if (dRoutes == 0) {
					GetCachedRoutes rcdao = new GetCachedRoutes(con);
					dRoutes = rcdao.getRoutes(rt, false).size();
				}
			
				rspMsg.setEntry("dispatchRoutes", String.valueOf(dRoutes));
			}
		} catch (DAOException de) {
			log.error("Error searching Schedule - " + de.getMessage(), de);
			rspMsg.setEntry("error", "Cannot search Flight Schedule - " + de.getMessage());
		} finally {
			ctx.release();
		}

		ctx.push(rspMsg);
	}
}