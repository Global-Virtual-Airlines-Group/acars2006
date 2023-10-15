// Copyright 2018, 2019, 2020, 2021, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;
import java.sql.Connection;

import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.RunwayListMessage;

import org.deltava.beans.Simulator;
import org.deltava.beans.navdata.*;
import org.deltava.beans.schedule.*;
import org.deltava.beans.stats.RunwayUsage;
import org.deltava.dao.*;
import org.deltava.util.system.SystemData;

/**
 * An ACARS data command to return popular runways for an Airport.
 * @author Luke
 * @version 11.1
 * @since 8.4
 */

public class PopularRunwaysCommand extends DataCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message and the airports
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		RoutePair rp = RoutePair.of(SystemData.getAirport(msg.getFlag("airportD")), SystemData.getAirport(msg.getFlag("airportA")));
		if (!rp.isPopulated()) {
			ctx.push(new AcknowledgeMessage(env.getOwner(), msg.getID(), "Unknown airports " + msg.getFlag("airportD") + " / " + msg.getFlag("airportA")));
			return;
		}
		
		// Get the sim version for the runway surface
		InfoMessage imsg = ctx.getACARSConnection().getFlightInfo();
		Simulator sim = (imsg == null) ? Simulator.P3Dv4 : imsg.getSimulator();
		
		// Create the response
		RunwayListMessage rspMsg = new RunwayListMessage(env.getOwner(), msg.getID(), true);
		rspMsg.setAirportD(rp.getAirportD()); rspMsg.setAirportA(rp.getAirportA());
		
		try {
			Connection con = ctx.getConnection();
			
			// Load runway surfaces
			GetNavData navdao = new GetNavData(con);
			List<Runway> dRwys = navdao.getRunways(rp.getAirportD(), sim);
			List<Runway> aRwys = navdao.getRunways(rp.getAirportA(), sim);
			
			// Load popular runways
			UsagePercentFilter rf = new UsagePercentFilter(10);
			GetRunwayUsage ardao = new GetRunwayUsage(con);
			RunwayUsage dru = ardao.getPopularRunways(rp, true);
			RunwayUsage aru = ardao.getPopularRunways(rp, false);
			rf.filter(dru.apply(dRwys)).forEach(rspMsg::add);
			rf.filter(aru.apply(aRwys)).forEach(rspMsg::add);

			// Push the response
			rspMsg.setMaxAge(4500);
			ctx.push(rspMsg);
		} catch (DAOException de) {
			log.atError().withThrowable(de).log("Error loading runway popularity - {}", de.getMessage());
			ctx.push(new AcknowledgeMessage(env.getOwner(), msg.getID(), "Cannot load runway list"));
		} finally {
			ctx.release();
		}
	}
}