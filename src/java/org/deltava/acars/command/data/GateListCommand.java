// Copyright 2018, 2019, 2020, 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;

import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.GateMessage;

import org.deltava.beans.Simulator;
import org.deltava.beans.navdata.Gate;
import org.deltava.beans.schedule.*;

import org.deltava.comparators.GateComparator;

import org.deltava.dao.*;
import org.deltava.util.EnumUtils;
import org.deltava.util.system.SystemData;

/**
 * An ACARS data command to list available airport gates.
 * @author Luke
 * @version 10.0
 * @since 8.4
 */

public class GateListCommand extends DataCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		GateMessage rspMsg = new GateMessage(env.getOwner(), msg.getID());		
		InfoMessage inf = ctx.getACARSConnection().getFlightInfo();
		Simulator sim = (inf == null) ? EnumUtils.parse(Simulator.class, msg.getFlag("sim"), Simulator.FSX) : inf.getSimulator();
		
		// Get the airport / airline / isDeparture
		boolean isDeparture = Boolean.valueOf(msg.getFlag("isDeparture")).booleanValue();
		rspMsg.setAirport(SystemData.getAirport(msg.getFlag("airport")));
		Airport aA = SystemData.getAirport(msg.getFlag("airportA"));
		Airline al = SystemData.getAirline(msg.getFlag("airline"));
		if (al == null)
			al = SystemData.getAirline(ctx.getACARSConnection().getUserData().getAirlineCode());
		
		// 1. Load all gates for Airport that have our Airline, sort based on popularity for route
		// 2. Load all gates for Airport that have our Airline, sort based on popularity 
		// 3. Load all gates for Airport, sort based on popularlity
		
		List<Gate> gates = new ArrayList<Gate>(); final Airline a = al;
		try {
			GetGates gdao = new GetGates(ctx.getConnection());
			gdao.setQueryMax(40);
			if ((rspMsg.getAirport() == null) && (inf != null) && inf.isPopulated()) {
				List<Gate> popGates = gdao.getPopularGates(inf, sim, isDeparture);
				popGates.stream().filter(g -> g.getAirlines().contains(a)).forEach(gates::add);
				rspMsg.setAirport(isDeparture ? inf.getAirportD() : inf.getAirportA());
				rspMsg.setRouteUsage(true);
			} else if ((rspMsg.getAirport() != null) && (aA != null)) {
				RoutePair rp = new ScheduleRoute(rspMsg.getAirport(), aA);
				List<Gate> popGates = gdao.getPopularGates(rp, sim, isDeparture);
				popGates.stream().filter(g -> g.getAirlines().contains(a)).forEach(gates::add);
				rspMsg.setAirport(isDeparture ? rp.getAirportD() : rp.getAirportA());
				rspMsg.setRouteUsage(true);
			}
				
			if (gates.isEmpty() && (rspMsg.getAirport() != null)) {
				List<Gate> allGates = gdao.getGates(rspMsg.getAirport(), sim);
				Collections.sort(allGates, new GateComparator(GateComparator.USAGE));
				gates.addAll(allGates);
			}
		} catch (DAOException de) {
			log.error("Error loading Gates - " + de.getMessage(), de);
		} finally {
			ctx.release();
		}
		
		// Sanity check the reply
		if (rspMsg.getAirport() == null) {
			log.warn(String.format("Unknown Airport - %s", msg.getFlag("airport")));
			return;
		}
		
		// Filter based on airline
		gates.stream().filter(g -> g.getAirlines().contains(a)).forEach(rspMsg::add);
		if (rspMsg.getResponse().isEmpty())
			rspMsg.addAll(gates);
		
		rspMsg.setMaxAge(5500);
		ctx.push(rspMsg);
	}
}