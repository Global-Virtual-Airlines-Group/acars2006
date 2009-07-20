// Copyright 2007, 2008, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import java.util.*;

import org.jdom.*;

import org.deltava.beans.Pilot;
import org.deltava.beans.navdata.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.dispatch.FlightDataMessage;

import org.deltava.util.StringUtils;
import org.deltava.util.system.SystemData;

/**
 * A Parser for DispatchInfo elements.
 * @author Luke
 * @version 2.6
 * @since 2.1
 */

class DispatchInfoParser extends ElementParser<FlightDataMessage> {

	/**
	 * Convert an XML dispatch request element into a FlightDataMessage.
	 * @param e the XML element
	 * @param user the originating user
	 * @return a FlightDataMessage
	 */
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
		msg.setAirportD(SystemData.getAirport(getChildText(fe, "airportD", null)));
		msg.setAirportA(SystemData.getAirport(getChildText(fe, "airportA", null)));
		msg.setAirportL(SystemData.getAirport(getChildText(fe, "airportL", null)));
		msg.setSID(getChildText(fe, "sid", null));
		msg.setSTAR(getChildText(fe, "star", null));
		msg.setTXCode(StringUtils.parse(getChildText(fe, "tx", "2200"), 2200));
		
		// Get the fuel data
		Element tse = e.getChild("fuel");
		for (Iterator<?> i = tse.getChildren().iterator(); i.hasNext(); ) {
			Element te = (Element) i.next();
			FuelTank tank = FuelTank.get(te.getAttributeValue("name"));
			msg.addFuel(tank, StringUtils.parse(te.getAttributeValue("load"), 0));
		}
		
		// Get the waypoints
		Element rse = e.getChild("route");
		msg.setRoute(rse.getChildTextTrim("text"));
		for (Iterator<?> i = rse.getChildren("waypoint").iterator(); i.hasNext(); ) {
			Element wpe = (Element) i.next();
			double lat = StringUtils.parse(wpe.getAttributeValue("lat"), 0.0);
			double lng = StringUtils.parse(wpe.getAttributeValue("lon"), 0.0);
			NavigationDataBean nd = NavigationDataBean.create(wpe.getAttributeValue("type"), lat, lng);
			nd.setCode(wpe.getAttributeValue("code"));
			nd.setRegion(wpe.getAttributeValue("region"));
			boolean inTR = Boolean.valueOf(wpe.getAttributeValue("tr")).booleanValue();
			if (inTR)
				nd.setAirway(".");
			else
				nd.setAirway(wpe.getAttributeValue("airway"));
			
			msg.addWaypoint(nd);
		}
		
		// Return the message
		return msg;
	}
}