// Copyright 2018, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;

import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.GateMessage;

import org.deltava.beans.Simulator;
import org.deltava.beans.navdata.Gate;
import org.deltava.beans.schedule.*;

import org.deltava.dao.*;

import org.deltava.util.system.SystemData;

/**
 * An ACARS data command to list available airport gates.
 * @author Luke
 * @version 8.6
 * @since 8.4
 */

public class GateListCommand extends DataCommand {

	/**
	 * Initializes the Command.
	 */
	public GateListCommand() {
		super(GateListCommand.class);
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
		GateMessage rspMsg = new GateMessage(env.getOwner(), msg.getID());		
		InfoMessage inf = ctx.getACARSConnection().getFlightInfo();
		Simulator sim = (inf == null) ? Simulator.FSX : inf.getSimulator();
		
		// Get the airport / airline / isDeparture
		boolean isDeparture = Boolean.valueOf(msg.getFlag("isDeparture")).booleanValue();
		rspMsg.setAirport(SystemData.getAirport(msg.getFlag("airport")));
		Airline al = SystemData.getAirline(msg.getFlag("airline"));
		if (al == null)
			al = SystemData.getAirline(ctx.getACARSConnection().getUserData().getAirlineCode());
		
		List<Gate> gates = new ArrayList<Gate>();
		try {
			GetGates gdao = new GetGates(ctx.getConnection());
			gdao.setQueryMax(20);
			if ((rspMsg.getAirport() == null) && (inf != null)) {
				gates.addAll(gdao.getPopularGates(inf, sim, isDeparture));
				rspMsg.setAirport(isDeparture ? inf.getAirportD() : inf.getAirportA());
			}
				
			if (gates.isEmpty())
				gates = gdao.getGates(rspMsg.getAirport(), sim);
		} catch (DAOException de) {
			log.error("Error loading Gates - " + de.getMessage(), de);
		} finally {
			ctx.release();
		}
		
		// Filter based on airline
		final Airline a = al;
		gates.stream().filter(g -> g.getAirlines().contains(a)).forEach(rspMsg::add);
		ctx.push(rspMsg);
	}
}