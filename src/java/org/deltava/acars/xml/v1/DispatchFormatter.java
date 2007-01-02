// Copyright 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1;

import org.jdom.Element;

import org.deltava.acars.message.*;

import org.deltava.util.*;

/**
 * An XML Formatter for Dispatch messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

class DispatchFormatter extends ElementFormatter {

	/**
	 * Formats a DispatchMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	public Element format(Message msg) {

		// Cast the message
		DispatchMessage dmsg = (DispatchMessage) msg;

		// Create the element
		Element e = initResponse(msg.getType());
		e.addContent(XMLUtils.createElement("flightCode", dmsg.getFlightCode()));
		e.addContent(XMLUtils.createElement("leg", String.valueOf(dmsg.getLeg())));
		e.addContent(XMLUtils.createElement("eqType", dmsg.getEquipmentType()));
		e.addContent(formatAirport(dmsg.getAirportD(), "airportD"));
		e.addContent(formatAirport(dmsg.getAirportA(), "airportA"));
		e.addContent(formatAirport(dmsg.getAirportL(), "airportL"));
		e.addContent(XMLUtils.createElement("route", dmsg.getRoute()));
		e.addContent(XMLUtils.createElement("fuel", String.valueOf(dmsg.getFuel())));
		e.addContent(XMLUtils.createElement("fuel", StringUtils.format(dmsg.getTXCode(), "0000")));
		return e;
	}
}