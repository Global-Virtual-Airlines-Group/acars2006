// Copyright 2007, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import java.util.*;

import org.jdom2.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.TerminalRouteMessage;

import org.deltava.beans.navdata.*;

import org.deltava.util.*;

/**
 * An XML Formatter for Terminal Route data messages.
 * @author Luke
 * @version 4.2
 * @since 2.0
 */

class TerminalRouteFormatter extends ElementFormatter {

	/**
	 * Formats a TerminalRouteMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
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
			
			// Build the waypoints
			StringBuilder buf = new StringBuilder(); StringBuilder rbuf = new StringBuilder();
			for (Iterator<NavigationDataBean> wi = tr.getWaypoints().iterator(); wi.hasNext(); ) {
				NavigationDataBean nd = wi.next();
				buf.append(nd.getCode());
				rbuf.append(nd.toString());
				if (wi.hasNext()) {
					buf.append(' ' );
					rbuf.append(' ');
				}
			}
			
			tre.addContent(XMLUtils.createElement("waypoints", buf.toString()));
			tre.addContent(XMLUtils.createElement("route", rbuf.toString()));
			e.addContent(tre);
		}
		
		return pe;
	}
}