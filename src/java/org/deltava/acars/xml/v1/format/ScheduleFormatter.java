// Copyright 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import java.util.*;

import org.jdom.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.ScheduleMessage;

import org.deltava.beans.schedule.ScheduleEntry;

import org.deltava.util.*;

/**
 * An XML Formatter for Flight Schedule messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

class ScheduleFormatter extends ElementFormatter {

	/**
	 * Formats a ScheduleMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	public Element format(Message msg) {

		// Cast the message
		ScheduleMessage smsg = (ScheduleMessage) msg;
		
		// Build the DataResponseElement
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "flights");
		
		// Add the flights
		for (Iterator<ScheduleEntry> i = smsg.getResponse().iterator(); i.hasNext(); ) {
			ScheduleEntry entry = i.next();
			Element se = new Element("flight");
			se.setAttribute("code", entry.getFlightCode());
			se.addContent(XMLUtils.createElement("airline", entry.getAirline().getCode()));
			se.addContent(XMLUtils.createElement("flight", StringUtils.format(entry.getFlightNumber(), "000")));
			se.addContent(XMLUtils.createElement("leg", String.valueOf(entry.getLeg())));
			se.addContent(XMLUtils.createElement("eqType", entry.getEquipmentType()));
			se.addContent(XMLUtils.createElement("timeD", StringUtils.format(entry.getTimeD(), "HH:mm")));
			se.addContent(XMLUtils.createElement("timeA", StringUtils.format(entry.getTimeA(), "HH:mm")));
			se.addContent(XMLUtils.createElement("distance", String.valueOf(entry.getDistance())));
			se.addContent(XMLUtils.createElement("length", StringUtils.format(entry.getLength()  / 10.0f, "#0.0")));
			se.addContent(formatAirport(entry.getAirportD(), "airportD"));
			se.addContent(formatAirport(entry.getAirportA(), "airportA"));
			e.addContent(se);
		}
		
		return pe;
	}
}