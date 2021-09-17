// Copyright 2018, 2019, 2020, 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;
import java.sql.Connection;

import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.RunwayListMessage;

import org.deltava.beans.Simulator;
import org.deltava.beans.navdata.*;
import org.deltava.beans.schedule.Airport;

import org.deltava.dao.*;
import org.deltava.util.system.SystemData;

/**
 * An ACARS data command to return popular runways for an Airport.
 * @author Luke
 * @version 10.1
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
		Airport aD = SystemData.getAirport(msg.getFlag("airportD"));
		Airport aA = SystemData.getAirport(msg.getFlag("airportA"));
		if ((aD == null) || (aA == null)) {
			ctx.push(new AcknowledgeMessage(env.getOwner(), msg.getID(), "Unknown airports " + msg.getFlag("airportD") + " / " + msg.getFlag("airportA")));
			return;
		}
		
		// Get the sim version for the runway surface
		InfoMessage imsg = ctx.getACARSConnection().getFlightInfo();
		Simulator sim = (imsg == null) ? Simulator.FSX : imsg.getSimulator();
		Map<String, Surface> sfcs = new HashMap<String, Surface>();
		
		// Create the response
		RunwayListMessage rspMsg = new RunwayListMessage(env.getOwner(), msg.getID(), true);
		rspMsg.setAirportD(aD); rspMsg.setAirportA(aA);
		
		try {
			Connection con = ctx.getConnection();
			
			// Load runway surfaces
			GetNavData navdao = new GetNavData(con);
			navdao.getRunways(aD, sim).forEach(rw -> sfcs.put(rw.getCode() + "-" + rw.getName(), rw.getSurface()));
			navdao.getRunways(aA, sim).forEach(rw -> sfcs.put(rw.getCode() + "-" + rw.getName(), rw.getSurface()));
			
			// Load popular runways
			GetACARSRunways ardao = new GetACARSRunways(con);
			rspMsg.addAll(ardao.getPopularRunways(aD, aA, true));
			rspMsg.addAll(ardao.getPopularRunways(aD, aA, false));
			
			// Apply the surfaces
			rspMsg.getResponse().forEach(rw -> rw.setSurface(sfcs.getOrDefault(rw.getCode() + "-" + rw.getName(), Surface.UNKNOWN)));
			rspMsg.setMaxAge(4500);
			ctx.push(rspMsg);
		} catch (DAOException de) {
			log.error("Error loading runway popularity - " + de.getMessage(), de);
			ctx.push(new AcknowledgeMessage(env.getOwner(), msg.getID(), "Cannot load runway list"));
		} finally {
			ctx.release();
		}
	}
}