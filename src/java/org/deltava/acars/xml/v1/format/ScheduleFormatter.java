// Copyright 2006, 2008, 2012, 2023, 2024 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import org.jdom2.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.ScheduleMessage;

import org.deltava.beans.schedule.*;

import org.deltava.util.*;

/**
 * An XML Formatter for Flight Schedule messages.
 * @author Luke
 * @version 11.2
 * @since 1.0
 */

class ScheduleFormatter extends ElementFormatter {

	/**
	 * Formats a ScheduleMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {

		// Cast the message
		ScheduleMessage smsg = (ScheduleMessage) msg;
		
		// Build the DataResponseElement
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "flights");
		
		// Add the flights
		for (ScheduleEntry entry : smsg.getResponse()) {
			Element se = new Element("flight");
			se.setAttribute("code", entry.getFlightCode());
			if (entry instanceof ScheduleSearchEntry sse)
				se.setAttribute("routes", String.valueOf(sse.getDispatchRoutes()));
			
			// Add values
			se.addContent(XMLUtils.createElement("airline", entry.getAirline().getCode()));
			se.addContent(XMLUtils.createElement("flight", StringUtils.format(entry.getFlightNumber(), "000")));
			se.addContent(XMLUtils.createElement("leg", String.valueOf(entry.getLeg())));
			se.addContent(formatAirport(entry.getAirportD(), "airportD"));
			se.addContent(formatAirport(entry.getAirportA(), "airportA"));
			se.addContent(XMLUtils.createElement("eqType", entry.getEquipmentType()));
			se.addContent(XMLUtils.createElement("timeD", StringUtils.format(entry.getTimeD(), "HH:mm")));
			se.addContent(XMLUtils.createElement("timeA", StringUtils.format(entry.getTimeA(), "HH:mm")));
			se.addContent(XMLUtils.createElement("distance", String.valueOf(entry.getDistance())));
			se.addContent(XMLUtils.createElement("length", StringUtils.format(entry.getLength()  / 10.0f, "#0.0")));
			XMLUtils.addIfPresent(se, XMLUtils.createIfPresent("remarks", entry.getRemarks()));
			e.addContent(se);
		}
		
		return pe;
	}
}