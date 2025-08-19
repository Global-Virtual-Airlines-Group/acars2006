// Copyright 2011, 2016, 2019, 2020, 2021, 2023, 2024, 2025 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;
import java.sql.Connection;
import java.time.Instant;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;

import org.deltava.beans.UserData;
import org.deltava.beans.econ.*;
import org.deltava.beans.flight.*;
import org.deltava.beans.schedule.*;

import org.deltava.dao.*;
import org.deltava.util.*;
import org.deltava.util.system.SystemData;

import org.gvagroup.common.SharedData;

/**
 * An ACARS Command to request a passenger load factor for a flight.
 * @author Luke
 * @version 12.2
 * @since 4.0
 */

public class LoadFactorCommand extends DataCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the message
		UserData ud = ctx.getACARSConnection().getUserData();
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());

		// Check for airport
		double loadFactor = -1;
		RoutePair rp = RoutePair.of(SystemData.getAirport(msg.getFlag("airportD")), SystemData.getAirport(msg.getFlag("airportA")));
		if (rp.isPopulated() && msg.hasFlag("eqType")) {
			try {
				Connection con = ctx.getConnection();

				// Check for diversion or pre-calculated load factor
				GetFlightReports frdao = new GetFlightReports(con);
				Optional<FlightReport> df = frdao.getDraftReports(env.getOwner().getID(), null, ud.getDB()).stream().filter(fr -> fr.matches(rp)).findFirst();
				FlightReport dfr = df.orElse(null);
				int pax = df.isPresent() ? dfr.getPassengers() : -1;
				boolean isDivert = df.isPresent() && dfr.hasAttribute(Attribute.DIVERT);
				ackMsg.setEntry("isDivert", String.valueOf(isDivert));
				if (pax > 0)
					ackMsg.setEntry("paxCount", String.valueOf(pax));

				// Load the aircraft
				GetAircraft acdao = new GetAircraft(con);
				Aircraft a = acdao.get(msg.getFlag("eqType"));
				if ((a == null) && df.isPresent())
					a = acdao.get(dfr.getEquipmentType());

				// Load the original flight, get the pax count if it's not already in the draft flight report
				if (a != null) {
					AircraftPolicyOptions opts = a.getOptions(ud.getAirlineCode());
					if (opts != null) {
						if (pax >= 0) {
							loadFactor = Math.max(1.0, pax * 1.0d / opts.getSeats());
							ackMsg.setEntry("extraPax", String.valueOf(pax > opts.getSeats()));
						}
					} else
						log.warn("No policy options for the {} in {}", a.getName(), ud.getAirlineCode());
				} else
					log.warn("Unknown aircraft type - {}", msg.getFlag("eqType"));
			} catch (DAOException de) {
				log.atError().withThrowable(de).log(de.getMessage());
			} finally {
				ctx.release();
			}
		}

		// Calculate flight load factor
		java.io.Serializable econ = SharedData.get(SharedData.ECON_DATA + ud.getAirlineCode());
		if ((econ != null) && (loadFactor < 0)) {
			final Instant today = Instant.now();
			LoadFactor lf = new LoadFactor((EconomyInfo) IPCUtils.reserialize(econ));
			loadFactor = lf.generate(today);
			ackMsg.setEntry("targetLoadFactor", StringUtils.format(lf.getTargetLoad(today), "0.00000"));
		} else if (loadFactor < 0) loadFactor = 1;

		ackMsg.setEntry("loadFactor", StringUtils.format(loadFactor, "0.00000"));
		ctx.push(ackMsg);
	}
}