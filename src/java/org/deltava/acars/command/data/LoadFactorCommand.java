// Copyright 2011, 2016, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;
import java.sql.Connection;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;

import org.deltava.beans.UserData;
import org.deltava.beans.econ.*;
import org.deltava.beans.flight.FlightReport;
import org.deltava.beans.schedule.*;

import org.deltava.dao.*;
import org.deltava.util.*;
import org.deltava.util.system.SystemData;

import org.gvagroup.common.SharedData;

/**
 * An ACARS Command to request a passenger load factor for a flight. 
 * @author Luke
 * @version 8.6
 * @since 4.0
 */

public class LoadFactorCommand extends DataCommand {

	private class SparseRoute implements RoutePair {
		private final Airport _aD;
		private final Airport _aA;
		
		protected SparseRoute(Airport aD, Airport aA) {
			super();
			_aD = aD;
			_aA = aA;
		}
		
		@Override
		public Airport getAirportD() {
			return _aD;
		}
		
		@Override
		public Airport getAirportA() {
			return _aA;
		}
		
		@Override
		public int getDistance() {
			throw new UnsupportedOperationException();
		}
	}
	
	/**
	 * Initializes the Command.
	 */
	public LoadFactorCommand() {
		super(LoadFactorCommand.class);
	}

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
		double loadFactor = -1;
		
		// Create the ACK
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
		
		// Check for airport
		RoutePair rp = new SparseRoute(SystemData.getAirport(msg.getFlag("airportD")), SystemData.getAirport(msg.getFlag("airportA")));
		if (rp.isPopulated() && msg.hasFlag("eqType")) {
			try {
				Connection con = ctx.getConnection();
				
				// Check for diversion
				GetFlightReports frdao = new GetFlightReports(con);
				Optional<FlightReport> dFlights = frdao.getDraftReports(env.getOwner().getID(), null, ud.getDB()).stream().filter(fr -> fr.hasAttribute(FlightReport.ATTR_DIVERT)).findFirst();
				FlightReport dfr = dFlights.orElse(null);
				boolean hasDivert = ((dfr != null) && rp.getAirportD().equals(dfr.getAirportD()) && rp.getAirportA().equals(dfr.getAirportA()));
				
				// Load the aircraft
				GetAircraft acdao = new GetAircraft(con);
				Aircraft a = acdao.get(msg.getFlag("eqType"));
				if (a == null)
					log.warn("Unknown aircraft type - " + msg.getFlag("eqType"));
				
				// Load the original diverted flight, get the pax count
				FlightReport ofr = hasDivert ? frdao.getDiversion(rp.getAirportD(), ud.getID(), ud.getDB()) : null;
				if ((ofr != null) && (a != null)) {
					loadFactor = Math.min(1.0, ofr.getPassengers() * 1.0d / a.getSeats());
					ackMsg.setEntry("isDivert", "true");
				}
			} catch (DAOException de) {
				log.error(de.getMessage(), de);
			} finally {
				ctx.release();
			}
		}
		
		// Calculate flight load factor
		java.io.Serializable econ = SharedData.get(SharedData.ECON_DATA + ud.getAirlineCode());
		if ((econ != null) && (loadFactor < 0)) {
			LoadFactor lf = new LoadFactor((EconomyInfo) IPCUtils.reserialize(econ));
			loadFactor = lf.generate();
		} else if (loadFactor < 0)
			loadFactor = 1;
			
		ackMsg.setEntry("loadFactor", StringUtils.format(loadFactor, "0.00000"));
		ctx.push(ackMsg);
	}
}