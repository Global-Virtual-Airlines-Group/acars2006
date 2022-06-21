// Copyright 2007, 2008, 2009, 2012, 2022 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.dispatch.RouteRequestMessage;

import org.deltava.acars.xml.*;

import org.deltava.util.system.SystemData;

/**
 * A parser for route request messages.
 * @author Luke
 * @version 10.2
 * @since 2.0
 */

class DispatchRouteParser extends XMLElementParser<RouteRequestMessage> {

	/**
	 * Convert an XML route request element into a RouteRequestMessage.
	 * @param e the XML element
	 * @return a RouteMessage
	 * @throws XMLException if a parse error occurs 
	 */
	@Override
	public RouteRequestMessage parse(org.jdom2.Element e, Pilot user) throws XMLException {
		
		// Create the message
		RouteRequestMessage msg = new RouteRequestMessage(user);
		msg.setAirportD(SystemData.getAirport(getChildText(e, "airportD", null)));
		msg.setAirportA(SystemData.getAirport(getChildText(e, "airportA", null)));
		msg.setExternalRoutes(Boolean.parseBoolean(e.getAttributeValue("external")));
		return msg;
	}
}