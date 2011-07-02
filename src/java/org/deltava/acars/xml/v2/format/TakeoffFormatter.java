// Copyright 2009, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

import org.jdom.Element;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.*;
import org.deltava.acars.xml.XMLElementFormatter;

import org.deltava.util.XMLUtils;

/**
 * A formatter for Takeoff/Touchdown messages.
 * @author Luke
 * @version 4.0
 * @since 2.8
 */

public class TakeoffFormatter extends XMLElementFormatter {

	/**
	 * Formats an TakeoffMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {
		
		// Cast the message
		TakeoffMessage tmsg = (TakeoffMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		pe.setAttribute("id", Long.toHexString(msg.getID()));
		pe.setAttribute("takeoff", String.valueOf(tmsg.isTakeoff()));
		
		// Format the pilot
		Pilot p = msg.getSender();
		Element ue = new Element("pilot");
		ue.setAttribute("id", p.getPilotCode());
		ue.setAttribute("dbID", Integer.toHexString(p.getID()));
		ue.addContent(XMLUtils.createElement("name", p.getName()));
		pe.addContent(ue);
		
		// Add the flight data
		pe.addContent(XMLUtils.createElement("flightCode", tmsg.getFlightCode()));
		pe.addContent(XMLUtils.createElement("rank", p.getRank().getName()));
		pe.addContent(XMLUtils.createElement("eqType", tmsg.getEquipmentType()));
		pe.addContent(XMLUtils.createElement("airportD", tmsg.getAirportD().getICAO()));
		pe.addContent(XMLUtils.createElement("airportA", tmsg.getAirportA().getICAO()));
		return pe;
	}
}