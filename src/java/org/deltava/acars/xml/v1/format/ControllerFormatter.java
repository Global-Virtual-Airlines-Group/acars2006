// Copyright 2006, 2009, 2010 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import org.jdom.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.ControllerMessage;

import org.deltava.beans.servinfo.Controller;
import org.deltava.util.StringUtils;

/**
 * An XML Formatter for Controller data messages.
 * @author Luke
 * @version 3.2
 * @since 1.0
 */

class ControllerFormatter extends ElementFormatter {

	/**
	 * Formats a ControllerMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	public Element format(Message msg) {

		// Cast the message
		ControllerMessage cmsg = (ControllerMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "atc");
		for (Controller ctr : cmsg.getResponse()) {
			Element ce = new Element("ctr");
			ce.setAttribute("code", ctr.getCallsign());
			ce.setAttribute("name", ctr.getName());
			ce.setAttribute("rating", ctr.getRating().getName());
			ce.setAttribute("type", ctr.getFacility().getName());
			ce.setAttribute("freq", ctr.getFrequency());
			ce.setAttribute("networkID", String.valueOf(ctr.getID()));
			ce.setAttribute("lat", StringUtils.format(ctr.getLatitude(), "##0.0000"));
			ce.setAttribute("lng", StringUtils.format(ctr.getLongitude(), "##0.0000"));
			e.addContent(ce);
		}
		
		return pe;
	}
}