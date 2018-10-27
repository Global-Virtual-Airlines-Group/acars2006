// Copyright 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

import org.jdom2.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.GateMessage;

import org.deltava.beans.navdata.Gate;
import org.deltava.util.*;

/**
 * An XML formatter for Gate data messages.
 * @author Luke
 * @version 8.4
 * @since 8.4
 */

class GateFormatter extends ElementFormatter {

	/**
	 * Formats a Gate bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {
		
		// Cast the message
		GateMessage gmsg = (GateMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "gates");
		e.setAttribute("icao", gmsg.getAirport().getICAO());
		for (Gate g : gmsg.getResponse()) {
			Element ge = new Element("gate");
			String n = g.getName().toUpperCase();
			ge.setAttribute("name", n.startsWith("GATE ") ? n.substring(5) : g.getName());
			ge.setAttribute("isIntl", String.valueOf(g.isInternational()));
			ge.setAttribute("lat", StringUtils.format(g.getLatitude(), "##0.0000"));
			ge.setAttribute("lng", StringUtils.format(g.getLongitude(), "##0.0000"));
			ge.setAttribute("hdg", String.valueOf(g.getHeading()));
			ge.setAttribute("type", g.getGateType().getDescription());
			ge.setAttribute("useCount", String.valueOf(g.getUseCount()));
			g.getAirlines().forEach(al -> ge.addContent(XMLUtils.createElement("airline", al.getCode(), false)));
			e.addContent(ge);
		}
		
		return pe;
	}
}