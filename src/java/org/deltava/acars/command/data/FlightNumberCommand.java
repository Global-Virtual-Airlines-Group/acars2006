// Copyright 2014, 2015, 2019, 2020, 2021, 2023, 2025 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;
import java.sql.Connection;

import org.deltava.beans.*;
import org.deltava.beans.flight.*;
import org.deltava.beans.schedule.*;

import org.deltava.dao.*;

import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;

import org.deltava.util.*;
import org.deltava.util.system.SystemData;

/**
 * An ACARS data command to return a flight number.
 * @author Luke
 * @version 12.0
 * @since 5.4
 */

public class FlightNumberCommand extends DataCommand {

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

		// Get the Airports / Time of Day
		int hour = StringUtils.parse(msg.getFlag("hour"), 12);
		Airport airportD = SystemData.getAirport(msg.getFlag("airportD"));
		Airport airportA = SystemData.getAirport(msg.getFlag("airportA"));
		if ((airportD == null) || (airportA == null)) {
			rspMsg.setEntry("error", "Unknown Airports");
			ctx.push(rspMsg);
			return;
		}
		
		// Create the route pair
		UserData ud = ctx.getACARSConnection().getUserData();
		String airlineCode = StringUtils.isEmpty(msg.getFlag("airline")) ? ud.getAirlineCode() : msg.getFlag("airline");
		ScheduleRoute rt = new ScheduleRoute(SystemData.getAirline(airlineCode), airportD, airportA);
		try {
			Connection con = ctx.getConnection();
			GetFlightReports frdao = new GetFlightReports(con);
			List<? extends Flight> pireps = frdao.getDraftReports(ud.getID(), rt, ud.getDB());
			Flight se = pireps.isEmpty() ? null : pireps.get(0); 
			
			// Check in schedule if no draft found
			if (se == null) {
				GetRawSchedule rsdao = new GetRawSchedule(con);
				GetSchedule sdao = new GetSchedule(con);
				sdao.setSources(rsdao.getSources(true, ud.getDB()));
				se = sdao.getFlightNumber(rt, hour, ud.getDB());
			}
			
			if (se != null) {
				rspMsg.setEntry("airline", se.getAirline().getCode());
				rspMsg.setEntry("flight", String.valueOf(se.getFlightNumber()));
				rspMsg.setEntry("leg", String.valueOf(se.getLeg()));
				
				// Calculate ETOPS
				ETOPS re = ETOPSHelper.classify(GeoUtils.greatCircle(airportD, airportA, GeoUtils.GC_SEGMENT_SIZE)).getResult();
				rspMsg.setEntry("etops", re.name());
				
				// Show departure time
				if (se instanceof FlightTimes ft) {
					if (ft.getTimeD() != null)
						rspMsg.setEntry("timeD", StringUtils.format(ft.getTimeD(), "HH:mm"));
				}
			}
		} catch (DAOException de) {
			log.atError().withThrowable(de).log("Error searching Schedule - {}", de.getMessage());
			rspMsg.setEntry("error", "Cannot search Flight Schedule - " + de.getMessage());
		} finally {
			ctx.release();
		}
		
		ctx.push(rspMsg);
	}
}