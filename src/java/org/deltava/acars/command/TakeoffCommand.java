// Copyright 2009, 2011, 2012, 2019, 2020, 2022, 2023, 2024 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;
import java.sql.Connection;

import org.apache.logging.log4j.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;
import org.deltava.beans.flight.LandingScorer;
import org.deltava.beans.navdata.*;
import org.deltava.beans.schedule.Airport;

import org.deltava.comparators.GeoComparator;

import org.deltava.dao.*;
import org.deltava.dao.acars.SetTakeoff;
import org.deltava.util.GeoUtils;
import org.deltava.util.system.SystemData;

/**
 * An ACARS command to process takeoff/touchdown messages.
 * @author Luke
 * @version 11.2
 * @since 2.8
 */

public class TakeoffCommand extends ACARSCommand {
	
	private static final Logger log = LogManager.getLogger(TakeoffCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the message
		TakeoffMessage msg = (TakeoffMessage) env.getMessage();
		ACARSConnection ac = ctx.getACARSConnection();
		InfoMessage info = ac.getFlightInfo();
		if (info == null) {
			log.warn("{} sending Takeoff message - no Flight Info found", ac.getUserID());
			return;
		}
		
		// Copy message data
		msg.setAirportD(info.getAirportD());
		msg.setAirportA(info.getAirportA());
		msg.setEquipmentType(info.getEquipmentType());
		msg.setFlightCode(info.getFlightCode());
		
		// Find the closest airport
		List<Airport> airports = new ArrayList<Airport>(SystemData.getAirports().values());
		Collections.sort(airports, new GeoComparator(msg));
		Airport closestAirport = airports.get(0);
		
		// Check if we're the closest
		Airport a = msg.isTakeoff() ? info.getAirportD() : info.getAirportA();
		if (!a.equals(closestAirport)) {
			int distance = a.distanceTo(closestAirport); 
			log.warn("Closest airport for Flight {} is {} implied airport is {} (distance = {} miles)", ac.getUserID(), closestAirport.getICAO(), a.getICAO(), Integer.valueOf(distance));
			if (distance > 15) {
				a = closestAirport;
				if (msg.isTakeoff())
					msg.setAirportD(a);
				else
					msg.setAirportA(a);
			}
		}
		
		try {
			Connection con = ctx.getConnection();
			SetTakeoff todao = new SetTakeoff(con);
			boolean isBounce = todao.logTakeoff(info.getFlightID(), msg.isTakeoff());

			// Get the runway
			GetNavData nvdao = new GetNavData(con);
			LandingRunways lr = nvdao.getBestRunway(a, info.getSimulator(), msg, msg.getHeading());
			Runway r = lr.getBestRunway();
			if ((r != null) && !isBounce) {
				int dist = r.distanceFeet(msg) - r.getThresholdLength();
				double delta = GeoUtils.delta(r.getHeading(), GeoUtils.course(r, msg));
				if (delta > 90)
					dist = -dist;
				
				// Build the ack
				AcknowledgeMessage ackMsg = new AcknowledgeMessage(msg.getSender(), msg.getID());
				ackMsg.setEntry("airport", a.getICAO());
				ackMsg.setEntry("rwy", r.getName());
				ackMsg.setEntry("distance", String.valueOf(dist));
				ackMsg.setEntry("threshold", String.valueOf(r.getThresholdLength()));
				ackMsg.setEntry("takeoff", String.valueOf(msg.isTakeoff()));
				
				// Calculate landing score
				if (!msg.isTakeoff()) {
					msg.setScore(LandingScorer.score(msg.getVSpeed(), dist));
					ackMsg.setEntry("score", String.valueOf(msg.getScore()));
				}
				
				ctx.push(ackMsg);
			}
			
			// Send out a system message to the others if not a bounce
			if (!isBounce) {
				Collection<ACARSConnection> cons = ctx.getACARSConnectionPool().getAll(c -> (c.getProtocolVersion() > 1) && c.isAuthenticated() && (c.getID() != ac.getID()));
				cons.forEach(c -> ctx.push(msg, c.getID(), false));
			}
		} catch (DAOException de) {
			log.atError().withThrowable(de).log("Cannnot log takeoff/landing - {}", de.getMessage());
		} finally {
			ctx.release();
		}
	}
}