// Copyright 2007, 2008, 2009, 2011, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import org.jdom2.*;

import org.deltava.beans.*;
import org.deltava.beans.navdata.*;
import org.deltava.beans.schedule.*;

import org.deltava.acars.message.dispatch.FlightDataMessage;
import org.deltava.acars.xml.XMLElementParser;

import org.deltava.util.StringUtils;
import org.deltava.util.system.SystemData;

/**
 * A Parser for DispatchInfo elements.
 * @author Luke
 * @version 5.1
 * @since 2.1
 */

class DispatchInfoParser extends XMLElementParser<FlightDataMessage> {

	/**
	 * Convert an XML dispatch request element into a FlightDataMessage.
	 * @param e the XML element
	 * @param user the originating user
	 * @return a FlightDataMessage
	 */
	@Override
	public FlightDataMessage parse(Element e, Pilot user) {
		FlightDataMessage msg = new FlightDataMessage(user);
		msg.setRecipient(getChildText(e, "recipient", null));
		
		// Get the flight Data
		Element fe = e.getChild("flight");
		msg.setRouteValid(Boolean.valueOf(fe.getAttributeValue("routeValid")).booleanValue());
		msg.setNoSave(Boolean.valueOf(fe.getAttributeValue("noSave")).booleanValue());
		msg.setRouteID(StringUtils.parse(fe.getAttributeValue("routeID"), 0));
		msg.setAirline(SystemData.getAirline(getChildText(fe, "airline", user.getAirlineCode())));
		msg.setFlight(StringUtils.parse(getChildText(fe, "flight", "1"), 1));
		msg.setLeg(StringUtils.parse(getChildText(fe, "leg", "1"), 1));
		msg.setCruiseAltitude(getChildText(fe, "cruiseAlt", "31000"));
		msg.setEquipmentType(getChildText(fe, "eqType", user.getEquipmentType()));
		msg.setSimulator(Simulator.fromName(getChildText(e, "sim", Simulator.FS9.name())));
		msg.setAirportD(SystemData.getAirport(getChildText(fe, "airportD", null)));
		msg.setAirportA(SystemData.getAirport(getChildText(fe, "airportA", null)));
		msg.setAirportL(SystemData.getAirport(getChildText(fe, "airportL", null)));
		msg.setSID(getChildText(fe, "sid", null));
		msg.setSTAR(getChildText(fe, "star", null));
		msg.setGateD(emptyGate(msg.getAirportD(), getChildText(fe, "gateD", null)));
		msg.setGateA(emptyGate(msg.getAirportA(), getChildText(fe, "gateA", null)));
		msg.setTXCode(StringUtils.parse(getChildText(fe, "tx", "2200"), 2200));
		
		// Get the fuel data
		Element tse = e.getChild("fuel");
		for (Element te : tse.getChildren()) {
			FuelTank tank = FuelTank.get(te.getAttributeValue("name"));
			msg.addFuel(tank, StringUtils.parse(te.getAttributeValue("load"), 0));
		}
		
		// Get the waypoints
		Element rse = e.getChild("route");
		msg.setRoute(rse.getChildTextTrim("text"));
		for (Element wpe : rse.getChildren("waypoint")) {
			double lat = StringUtils.parse(wpe.getAttributeValue("lat"), 0.0);
			double lng = StringUtils.parse(wpe.getAttributeValue("lon"), 0.0);
			Navaid nt = Navaid.fromName(wpe.getAttributeValue("type"));
			NavigationDataBean nd = NavigationDataBean.create(nt, lat, lng);
			nd.setCode(wpe.getAttributeValue("code"));
			nd.setRegion(wpe.getAttributeValue("region"));
			boolean inTR = Boolean.valueOf(wpe.getAttributeValue("tr")).booleanValue();
			if (inTR)
				nd.setAirway(".");
			else
				nd.setAirway(wpe.getAttributeValue("airway"));
			
			msg.addWaypoint(nd);
		}
		
		return msg;
	}
	
	/*
	 * Creates an undefined gate.
	 */
	private static Gate emptyGate(ICAOAirport a, String id) {
		if (a == null) return null;
		
		Gate g = new Gate(0, 0);
		g.setCode(a.getICAO());
		g.setName(id);
		return g;
	}
}