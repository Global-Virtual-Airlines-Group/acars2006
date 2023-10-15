// Copyright 2009, 2010, 2015, 2019, 2020, 2021, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;
import java.sql.Connection;
import java.time.LocalDate;

import org.deltava.beans.Simulator;
import org.deltava.beans.acars.TaxiTime;
import org.deltava.beans.schedule.*;
import org.deltava.beans.stats.RunwayUsage;
import org.deltava.beans.navdata.*;
import org.deltava.beans.wx.METAR;

import org.deltava.comparators.RunwayComparator;

import org.deltava.dao.*;

import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.AirportInfoMessage;

import org.deltava.util.system.SystemData;

/**
 * An ACARS data command to return Airport weather and runway choices.
 * @author Luke
 * @version 11.1
 * @since 2.6
 */

public class AirportInfoCommand extends DataCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the message and the airport
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		Airport aD = SystemData.getAirport(msg.getFlag("airportD"));
		Airport aA = SystemData.getAirport(msg.getFlag("airportA"));
		if (aD == null)
			return;
		
		try {
			Connection c = ctx.getConnection();
			
			// Get the weather
			GetWeather wxdao = new GetWeather(c);
			METAR wxD = wxdao.getMETAR(aD.getICAO());
			UsageWindFilter rf = new UsageWindFilter(10, -7);
			rf.setWinds(wxD);
			
			// Get the runways
			GetNavData nddao = new GetNavData(c);
			GetRunwayUsage rwdao = new GetRunwayUsage(c);
			List<Runway> rwys = nddao.getRunways(aD, Simulator.P3Dv4);
			RunwayUsage dru = rwdao.getPopularRunways(RoutePair.of(aD, aA), true);
			
			// Convert to RunwayUsage and sort with weather/usage in mind
			List<RunwayUse> rwyD = dru.apply(rwys);
			if (wxD != null)
				rwyD.sort(new RunwayComparator(wxD.getWindDirection(), wxD.getWindSpeed(), true));
			
			// Get the taxi times
			final int year = LocalDate.now().getYear();
			GetACARSTaxiTimes ttdao = new GetACARSTaxiTimes(c);
			TaxiTime ttD = ttdao.getTaxiTime(aD, year);
			
			// Build the departure airport response
			AirportInfoMessage msgD = new AirportInfoMessage(env.getOwner(), msg.getID());
			msgD.setAirport(aD);
			msgD.setMETAR(wxD);
			rwyD.forEach(msgD::add);
			msgD.setTaxiTime(ttD);
			ctx.push(msgD);
			
			// Build the arrival airport response
			if (aA != null) {
				TaxiTime ttA = ttdao.getTaxiTime(aA, year);
				METAR wxA = wxdao.getMETAR(aA.getICAO());
				rf.setWinds(wxA);
				
				// Get runways
				rwys = nddao.getRunways(aA, Simulator.P3Dv4);
				RunwayUsage aru = rwdao.getPopularRunways(RoutePair.of(aD, aA), false);
				List<RunwayUse> rwyA = aru.apply(rwys);
				if (wxA != null)
					rwyA.sort(new RunwayComparator(wxA.getWindDirection(), wxA.getWindSpeed(), true));
				
				AirportInfoMessage msgA = new AirportInfoMessage(env.getOwner(), msg.getID());	
				msgA.setAirport(aA);
				msgA.setMETAR(wxA);
				rwyA.forEach(msgA::add);
				msgA.setTaxiTime(ttA);
				ctx.push(msgA);
			}
		} catch (DAOException de) {
			log.atError().withThrowable(de).log("Error getting Airport information - {}", de.getMessage());
			ctx.push(new AcknowledgeMessage(env.getOwner(), msg.getID(), "Cannot fetch Airport info - " + de.getMessage()));
		} finally {
			ctx.release();
		}
	}
}