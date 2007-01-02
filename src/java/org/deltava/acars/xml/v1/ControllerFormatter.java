// Copyright 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1;

import java.util.Iterator;

import org.jdom.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.ControllerMessage;

import org.deltava.beans.servinfo.Controller;

/**
 * An XML Formatter for Controller data messages.
 * @author Luke
 * @version 1.0
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
		for (Iterator<Controller> i = cmsg.getResponse().iterator(); i.hasNext(); ) {
			Controller ctr = i.next();
			
			// Build the element
			Element ce = new Element("ctr");
			ce.setAttribute("code", ctr.getCallsign());
			ce.setAttribute("name", ctr.getName());
			ce.setAttribute("rating", ctr.getRatingName());
			ce.setAttribute("type", ctr.getFacilityType());
			ce.setAttribute("freq", ctr.getFrequency());
			ce.setAttribute("networkID", String.valueOf(ctr.getID()));
			e.addContent(ce);
		}
		
		return pe;
	}
}