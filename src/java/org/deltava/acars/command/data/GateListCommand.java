// Copyright 2018, 2019, 2020, 2021, 2022, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;

import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.GateMessage;

import org.deltava.beans.navdata.Gate;
import org.deltava.beans.schedule.*;

import org.deltava.comparators.GateComparator;

import org.deltava.dao.*;
import org.deltava.util.system.SystemData;

/**
 * An ACARS data command to list available airport gates.
 * @author Luke
 * @version 11.1
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
		
		// Get the airport / airline / isDeparture
		boolean isDeparture = Boolean.parseBoolean(msg.getFlag("isDeparture"));
		rspMsg.setAirport(SystemData.getAirport(msg.getFlag("airport")));
		Airport aA = SystemData.getAirport(msg.getFlag("airportA"));
		Airline al = SystemData.getAirline(msg.getFlag("airline"));
		if (al == null)
			al = SystemData.getAirline(ctx.getACARSConnection().getUserData().getAirlineCode());
		
		try {
			GetGates gdao = new GetGates(ctx.getConnection());
			gdao.setQueryMax(40);
			if ((rspMsg.getAirport() == null) && (inf != null) && inf.isPopulated()) {
				GateHelper gh = new GateHelper(inf, al, 40, false);
				rspMsg.setRouteUsage(true);
				if (isDeparture) {
					gh.addDepartureGates(gdao.getGates(inf.getAirportD()), gdao.getUsage(inf, true, ctx.getDB()));
					rspMsg.setAirport(inf.getAirportD());
					rspMsg.addAll(gh.getDepartureGates());
				} else {
					gh.addArrivalGates(gdao.getGates(inf.getAirportA()), gdao.getUsage(inf, false, ctx.getDB()));
					rspMsg.setAirport(inf.getAirportA());
					rspMsg.addAll(gh.getArrivalGates());
				}
			} else if ((rspMsg.getAirport() != null) && (aA != null)) {
				RoutePair rp = RoutePair.of(rspMsg.getAirport(), aA);
				GateHelper gh = new GateHelper(rp, al, 40, false);
				rspMsg.setRouteUsage(true);
				if (isDeparture) {
					gh.addDepartureGates(gdao.getGates(rp.getAirportD()), gdao.getUsage(rp, true, ctx.getDB()));
					rspMsg.setAirport(rp.getAirportD());
					rspMsg.addAll(gh.getDepartureGates());
				} else {
					gh.addArrivalGates(gdao.getGates(rp.getAirportA()), gdao.getUsage(rp, false, ctx.getDB()));
					rspMsg.setAirport(rp.getAirportA());
					rspMsg.addAll(gh.getArrivalGates());
				}
			}
				
			if (rspMsg.getResponse().isEmpty() && (rspMsg.getAirport() != null)) {
				List<Gate> allGates = gdao.getGates(rspMsg.getAirport());
				Collections.sort(allGates, new GateComparator(GateComparator.USAGE));
				rspMsg.addAll(allGates);
			}
		} catch (DAOException de) {
			log.atError().withThrowable(de).log("Error loading Gates - {}", de.getMessage());
		} finally {
			ctx.release();
		}
		
		// Sanity check the reply
		if (rspMsg.getAirport() == null) {
			log.warn("Unknown Airport - {}", msg.getFlag("airport"));
			return;
		}
		
		rspMsg.setMaxAge(5500);
		ctx.push(rspMsg);
	}
}