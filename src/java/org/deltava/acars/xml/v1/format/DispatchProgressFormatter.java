// Copyright 2007, 2012, 2015, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import org.jdom2.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.dispatch.ProgressResponseMessage;

import org.deltava.beans.navdata.FIR;

import org.deltava.util.*;

/**
 * An XML Formatter for DispatchProgressResponse messages.
 * @author Luke
 * @version 8.4
 * @since 2.1
 */

class DispatchProgressFormatter extends ElementFormatter {

	/**
	 * Formats a ProgressResponseMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {
		
		// Cast the message
		ProgressResponseMessage rspmsg = (ProgressResponseMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, rspmsg.getRequestType().getCode());
		e.setAttribute("lat", StringUtils.format(rspmsg.getLocation().getLatitude(), "#0.00000"));
		e.setAttribute("lng", StringUtils.format(rspmsg.getLocation().getLongitude(), "##0.00000"));
		e.setAttribute("alt", String.valueOf(rspmsg.getLocation().getAltitude()));
		e.addContent(XMLUtils.createElement("originator", rspmsg.getRecipient()));
		e.addContent(formatAirport(rspmsg.getAirportD(), "airportD"));
		e.addContent(formatAirport(rspmsg.getAirportA(), "airportA"));
		if (rspmsg.getAirportL() != null)
			e.addContent(formatAirport(rspmsg.getAirportL(), "airportL"));
		e.addContent(XMLUtils.createElement("eqType", rspmsg.getEquipmentType()));
		e.addContent(XMLUtils.createElement("fuel", String.valueOf(rspmsg.getFuel())));
		e.addContent(XMLUtils.createElement("fuelFlow", String.valueOf(rspmsg.getBurnRate())));
		e.addContent(XMLUtils.createElement("groundSpeed", String.valueOf(rspmsg.getGroundSpeed())));
		if (rspmsg.getFIR() != null) {
			FIR f = rspmsg.getFIR();
			Element fe = XMLUtils.createElement("fir", f.getName());
			fe.setAttribute("id", f.getID());
			fe.setAttribute("aux", String.valueOf(f.isAux()));
			fe.setAttribute("oceanic", String.valueOf(f.isOceanic()));
			e.addContent(fe);
		}
		
		Element ae = new Element("alternates");
		rspmsg.getClosestAirports().forEach(a -> ae.addContent(formatAirport(a, "alt")));
		e.addContent(ae);
		return pe;
	}
}