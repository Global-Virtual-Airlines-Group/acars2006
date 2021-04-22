// Copyright 2006, 2009, 2010, 2011, 2012, 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import org.jdom2.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.ControllerMessage;

import org.deltava.beans.servinfo.*;

import org.deltava.util.StringUtils;

/**
 * An XML Formatter for Controller data messages.
 * @author Luke
 * @version 10.0
 * @since 1.0
 */

class ControllerFormatter extends ElementFormatter {

	/**
	 * Formats a ControllerMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {

		// Cast the message
		ControllerMessage cmsg = (ControllerMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "atc");
		e.setAttribute("network", cmsg.getNetwork().toString());
		for (Controller ctr : cmsg.getResponse()) {
			Element ce = new Element("ctr");
			ce.setAttribute("code", ctr.getCallsign());
			ce.setAttribute("name", ctr.getName());
			ce.setAttribute("rating", ctr.getRating().getName());
			ce.setAttribute("type", ctr.getFacility().getName());
			ce.setAttribute("facility", String.valueOf(ctr.getFacility().ordinal()));
			ce.setAttribute("freq", ctr.getFrequency());
			ce.setAttribute("networkID", String.valueOf(ctr.getID()));
			if (ctr.getPosition() != null) {
				ce.setAttribute("lat", StringUtils.format(ctr.getLatitude(), "##0.0000"));
				ce.setAttribute("lng", StringUtils.format(ctr.getLongitude(), "##0.0000"));
				ce.setAttribute("range", String.valueOf(ctr.getRange()));
			}
			
			// VATSIM multiple radio positions
			for (RadioPosition rp : ctr.getRadios()) {
				Element re = new Element("radio");
				re.setAttribute("id", String.valueOf(rp.getSequence()));
				re.setAttribute("lat", StringUtils.format(rp.getLatitude(), "##0.0000"));
				re.setAttribute("lng", StringUtils.format(rp.getLongitude(), "##0.0000"));
				re.setAttribute("alt", String.valueOf(rp.getAltitude()));
				re.setAttribute("freq", rp.getFrequency());
				ce.addContent(re);
			}
			
			e.addContent(ce);
		}
		
		return pe;
	}
}