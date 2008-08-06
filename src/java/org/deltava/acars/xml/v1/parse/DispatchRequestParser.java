// Copyright 2007, 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import java.util.*;

import org.jdom.Element;

import org.deltava.beans.Pilot;
import org.deltava.beans.schedule.GeoPosition;

import org.deltava.acars.beans.FuelTank;
import org.deltava.acars.message.dispatch.RequestMessage;

import org.deltava.util.StringUtils;
import org.deltava.util.system.SystemData;

/**
 * A parser for DispatchRequest elements.
 * @author Luke
 * @version 2.2
 * @since 2.0
 */

class DispatchRequestParser extends ElementParser<RequestMessage> {

	/**
	 * Convert an XML dispatch request element into a DispatchRequestMessage.
	 * @param e the XML element
	 * @return a RequestMessage
	 */
	public RequestMessage parse(Element e, Pilot user) {
		
		// Create the message
		RequestMessage msg = new RequestMessage(user);
		
		// Get the max gross weight of the aircraft
		Element ie = e.getChild("info");
		msg.setMaxWeight(StringUtils.parse(getChildText(ie, "maxweight", null), 0));
		msg.setZeroFuelWeight(StringUtils.parse(getChildText(ie, "emptyweight", null), 0));
		
		// Get latitude/longitude
		double lat = StringUtils.parse(ie.getAttributeValue("lat"), 0.0d);
		double lng = StringUtils.parse(ie.getAttributeValue("lng"), 0.0d);
		msg.setLocation(new GeoPosition(lat, lng));
		
		// Get the route/equipment data
		msg.setAirline(SystemData.getAirline(getChildText(ie, "airline", null)));
		msg.setAirportD(SystemData.getAirport(getChildText(ie, "airportD", null)));
		msg.setAirportA(SystemData.getAirport(getChildText(ie, "airportA", null)));
		msg.setAirportL(SystemData.getAirport(getChildText(ie, "airportL", null)));
		msg.setEquipmentType(getChildText(ie, "eqtype", user.getEquipmentType()));
		
		// Get the fuel tanks
		Element tse = e.getChild("tanks");
		if (tse != null) {
			for (Iterator i = tse.getChildren().iterator(); i.hasNext(); ) {
				Element te = (Element) i.next();
				FuelTank tank = FuelTank.get(te.getAttributeValue("name"));
				msg.addTank(tank, StringUtils.parse(te.getAttributeValue("size"), 0));
			}
		}
		
		return msg;
	}
}