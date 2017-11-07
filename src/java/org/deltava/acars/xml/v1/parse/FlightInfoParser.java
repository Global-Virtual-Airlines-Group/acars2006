// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2014, 2016, 2017 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import java.time.Instant;

import org.jdom2.Element;
import org.apache.log4j.Logger;

import org.deltava.beans.*;
import org.deltava.acars.beans.TXCode;
import org.deltava.acars.message.*;
import org.deltava.acars.xml.*;

import org.deltava.util.StringUtils;
import org.deltava.util.system.SystemData;

/**
 * A Parser for Flight Information elements.
 * @author Luke
 * @version 8.0
 * @since 1.0
 */

class FlightInfoParser extends XMLElementParser<InfoMessage> {
	
	// FSUIPC Flight Simulator version constants - 2006=FSX, 2008=Prepar3D/ESP, 2017=Prepar3Dv4
	private static final int[] FSUIPC_FS_VERSIONS = {95, 98, 2000, 0, 0, 0, 2002, 2004, 2006, 2008, 2008, 0, 2017};
	
	private static final Logger log = Logger.getLogger(FlightInfoParser.class);

	/**
	 * Convert an XML flight information element into an InfoMessage.
	 * @param e the XML element
	 * @return an InfoMessage
	 * @throws XMLException if a parse error occurs 
	 */
	@Override
	public InfoMessage parse(Element e, Pilot user) throws XMLException {

		// Create the bean
		InfoMessage msg = new InfoMessage(user);

		// Parse the start date/time
		try {
			Instant dt = StringUtils.parseInstant(getChildText(e, "startTime", ""), "MM/dd/yyyy HH:mm:ss");
			if (dt.toEpochMilli() > (System.currentTimeMillis() + 86400_000))
				throw new IllegalArgumentException("Start date/time too far in future - " + dt);
				
			msg.setStartTime(dt);
		} catch (Exception ex) {
			msg.setStartTime(Instant.now());
		}

		// Load the flight code
		String fCode = getChildText(e, "flight_num", SystemData.get("airline.code") + "001");
		if (!Character.isLetter(fCode.charAt(0)))
			fCode = SystemData.get("airline.code") + fCode;
		int ofs = fCode.indexOf(" LEG");
		if (ofs > 0)
			fCode = fCode.substring(0, ofs).trim(); 

		// Load the bean
		msg.setFlightID(StringUtils.parse(getChildText(e, "flight_id", "0"), 0));
		msg.setEquipmentType(getChildText(e, "equipment", "UNKNOWN"));
		msg.setFlightCode(fCode);
		msg.setAltitude(getChildText(e, "cruise_alt", null));
		msg.setTailCode(getChildText(e, "tailCode", null));
		msg.setComments(getChildText(e, "remarks", null));
		msg.setAirportD(getAirport(getChildText(e, "airportD", null)));
		msg.setAirportA(getAirport(getChildText(e, "airportA", null)));
		msg.setAirportL(SystemData.getAirport(getChildText(e, "airportL", null)));
		msg.setCheckRide(Boolean.valueOf(getChildText(e, "checkRide", null)).booleanValue());
		msg.setNoRideCheck(Boolean.valueOf(getChildText(e, "noRideCheck", null)).booleanValue());
		msg.setComplete(Boolean.valueOf(getChildText(e, "complete", null)).booleanValue());
		msg.setDispatchPlan(Boolean.valueOf(getChildText(e, "dispatchPlan", "false")).booleanValue());
		msg.setScheduleValidated(Boolean.valueOf(getChildText(e, "scheduleValidated", "false")).booleanValue());
		msg.setDispatcherID(StringUtils.parse(getChildText(e, "dispatcherID", "0"), 0));
		msg.setRouteID(StringUtils.parse(getChildText(e, "routeID", "0"), 0));
		msg.setNetwork(OnlineNetwork.fromName(getChildText(e, "network", null)));
		msg.setTX(StringUtils.parse(getChildText(e, "tx", String.valueOf(TXCode.DEFAULT_IFR)), TXCode.DEFAULT_IFR));
		
		// Parse the simulator
		String sim = getChildText(e, "fs_ver", "2004");
		int ver = StringUtils.parse(sim, 2004);
		if ((ver > 0) && (ver < FSUIPC_FS_VERSIONS.length))
			msg.setSimulator(Simulator.fromVersion(FSUIPC_FS_VERSIONS[ver], Simulator.UNKNOWN));
		else
			msg.setSimulator(Simulator.fromVersion(ver, Simulator.UNKNOWN));
		
		// Warn if unknown simulator
		if (msg.getSimulator() == Simulator.UNKNOWN) {
			log.warn("Unknown simulator version from " + msg.getSender().getPilotCode() + " - " + sim);
			msg.setSimulator(Simulator.XP10); // FIXME: Hack for Build 120/121 not detecting XP all the time
		}
		
		// Load sim major/minor
		String simVersion = getChildText(e, "simVersion", "0.0"); int pos = simVersion.indexOf('.');
		int major = StringUtils.parse(simVersion.substring(0, pos), 0); int minor = StringUtils.parse(simVersion.substring(pos + 1), 0);
		if (major != 0)
			msg.setSimulatorVersion(major, minor);
		else if (msg.getSimulator() == Simulator.FS9)
			msg.setSimulatorVersion(9, 1);
		else if (msg.getSimulator() == Simulator.FS2002)
			msg.setSimulatorVersion(8, 0);
		
		// Read pax (122+) / load factors (121+) if present
		msg.setPassengers(StringUtils.parse(getChildText(e, "pax", "0"), 0));
		if (msg.getPassengers() == 0) {
			String lf = getChildText(e, "loadFactor", "0.0");
			msg.setLoadFactor(StringUtils.parse(lf, 0d));
			if (Double.isNaN(msg.getLoadFactor()))
				log.warn("Invalid (NaN) load factor from " + user.getPilotCode() + " - " + lf);
		}
		
		// Load SID data
		Element sid = e.getChild("sid");
		if (sid != null)
			msg.setSID(getChildText(sid, "name", "") + "." + getChildText(sid, "transition", "") + "." + getChildText(sid, "rwy", ""));
		
		// Load STAR data
		Element star = e.getChild("star");
		if (star != null)
			msg.setSTAR(getChildText(star, "name", "") + "." + getChildText(star, "transition", "") + "." + getChildText(star, "rwy", ""));

		// Load waypoints
		msg.setWaypoints(getChildText(e, "route", "DIRECT"));
		return msg;
	}
}