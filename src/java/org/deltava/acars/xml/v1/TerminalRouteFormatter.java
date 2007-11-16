// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1;

import java.util.*;

import org.jdom.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.TerminalRouteMessage;

import org.deltava.beans.navdata.TerminalRoute;

import org.deltava.util.*;

/**
 * An XML Formatter for Terminal Route data messages.
 * @author Luke
 * @version 2.0
 * @since 2.0
 */

class TerminalRouteFormatter extends ElementFormatter {

	/**
	 * Formats a TerminalRouteMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	public Element format(Message msg) {
		
		// Cast the message
		TerminalRouteMessage trmsg = (TerminalRouteMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "sid_stars");
		for (Iterator<TerminalRoute> i = trmsg.getResponse().iterator(); i.hasNext(); ) {
			TerminalRoute tr = i.next();
			
			// Build the route element
			Element tre = new Element("troute");
			tre.setAttribute("airport", tr.getICAO());
			tre.setAttribute("name", tr.getName());
			tre.setAttribute("type", tr.getTypeName());
			tre.setAttribute("transition", tr.getTransition());
			tre.setAttribute("runway", tr.getRunway());
			tre.addContent(XMLUtils.createElement("waypoints", StringUtils.listConcat(tr.getWaypoints(), " ")));
			e.addContent(tre);
		}
		
		return pe;
	}
}