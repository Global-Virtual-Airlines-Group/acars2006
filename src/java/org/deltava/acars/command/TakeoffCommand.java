// Copyright 2009, 2011, 2012, 2019, 2020, 2022 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;
import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;
import org.deltava.beans.GeoLocation;
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
 * @version 10.3
 * @since 2.8
 */

public class TakeoffCommand extends ACARSCommand {
	
	private static final Logger log = Logger.getLogger(TakeoffCommand.class);

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
			log.error(ac.getUserID() + " sending Takeoff message - no Flight Info found");
			return;
		}
		
		// Copy message data
		msg.setAirportD(info.getAirportD());
		msg.setAirportA(info.getAirportA());
		msg.setEquipmentType(info.getEquipmentType());
		msg.setFlightCode(info.getFlightCode());
		
		// Find the closest airport
		List<Airport> airports = new ArrayList<Airport>(SystemData.getAirports().values());
		Collections.sort(airports, new GeoComparator(msg.getLocation()));
		Airport closestAirport = airports.get(0);
		
		// Check if we're the closest
		Airport a = msg.isTakeoff() ? info.getAirportD() : info.getAirportA();
		if (!a.equals(closestAirport)) {
			int distance = a.distanceTo(closestAirport); 
			log.warn("Closest airport for Flight " + info.getID() + " is " + closestAirport.getICAO() + " implied airport is " + a.getICAO() + " (distance = " + distance + " miles)");
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
			LandingRunways lr = nvdao.getBestRunway(a, info.getSimulator(), msg.getLocation(), msg.getHeading());
			Runway r = lr.getBestRunway();
			if ((r != null) && !isBounce) {
				GeoLocation rw = (r.getThresholdLength() > 0) ? r.getThreshold() : r;
				int dist = r.distanceFeet(msg.getLocation()) - r.getThresholdLength();
				double delta = GeoUtils.delta(r.getHeading(), GeoUtils.course(rw, msg.getLocation()));
				if (delta > 90)
					dist = -dist;
				
				// Send the ack
				AcknowledgeMessage ackMsg = new AcknowledgeMessage(msg.getSender(), msg.getID());
				ackMsg.setEntry("airport", a.getICAO());
				ackMsg.setEntry("rwy", r.getName());
				ackMsg.setEntry("distance", String.valueOf(dist));
				ackMsg.setEntry("takeoff", String.valueOf(msg.isTakeoff()));
				ctx.push(ackMsg);
			}
			
			// Send out a system message to the others if not a bounce
			if (!isBounce) {
				String aCode = ac.getUserData().getAirlineCode();
				Collection<ACARSConnection> cons = ctx.getACARSConnectionPool().getAll(c -> (c.getProtocolVersion() > 1) && c.isAuthenticated() && aCode.equals(c.getUserData().getAirlineCode()));
				cons.forEach(c -> ctx.push(msg, c.getID(), false));
			}
		} catch (DAOException de) {
			log.error("Cannnot log takeoff/landing - " + de.getMessage(), de);
		} finally {
			ctx.release();
		}
	}
}