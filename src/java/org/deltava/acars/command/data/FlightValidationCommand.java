// Copyright 2008, 2009, 2011, 2012, 2014, 2015 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;
import java.util.stream.Collectors;
import java.sql.Connection;

import org.deltava.beans.*;
import org.deltava.beans.flight.*;
import org.deltava.beans.schedule.*;
import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.dao.*;
import org.deltava.util.GeoUtils;
import org.deltava.util.system.SystemData;

/**
 * An ACARS Command to validate that a route exists in the Flight Schedule or is part of a
 * valid flight assignment or charter request, and if any dispatch routes currently exist.
 * @author Luke
 * @version 5.5
 * @since 2.3
 */

public class FlightValidationCommand extends DataCommand {

	/**
	 * Initializes the Command.
	 */
	public FlightValidationCommand() {
		super(FlightValidationCommand.class);
	}

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
			ctx.push(rspMsg, env.getConnectionID());
			return;
		}
		
		// Create the route pair and do ETOPS validation
		ScheduleRoute rt = new ScheduleRoute(SystemData.getAirline(ud.getAirlineCode()), airportD, airportA);
		Collection<GeoLocation> gc = GeoUtils.greatCircle(airportD, airportA, 25);
		ETOPS e = ETOPSHelper.classify(gc).getResult();
		rspMsg.setEntry("etops", String.valueOf(ETOPSHelper.validate(null, e)));
		
		try {
			Connection con = ctx.getConnection();

			// Check the route
			GetSchedule sdao = new GetSchedule(con);
			FlightTime flightTime = sdao.getFlightTime(rt, ud.getDB());
			boolean inSchedule = (flightTime.getFlightTime() > 0); boolean isValid = inSchedule;
			
			// Check for draft flights if not in schedule
			if (!inSchedule) {
				GetFlightReports frdao = new GetFlightReports(con);
				List<FlightReport> pireps = frdao.getDraftReports(ud.getID(), rt, ud.getDB());
				List<FlightReport> fp = pireps.stream().filter(fr -> (fr.hasAttribute(FlightReport.ATTR_CHARTER) || (fr.getDatabaseID(DatabaseID.ASSIGN) != 0))).collect(Collectors.toList());
				isValid = (fp.size() > 0);
			}
			
			rspMsg.setEntry("routeOK", String.valueOf(isValid));
			rspMsg.setEntry("hasCurrent", String.valueOf(flightTime.hasCurrent()));
			rspMsg.setEntry("hasHistoric", String.valueOf(flightTime.hasHistoric()));
			
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

		ctx.push(rspMsg, env.getConnectionID());
	}
}