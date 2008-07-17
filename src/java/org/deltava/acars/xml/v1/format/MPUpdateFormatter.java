// Copyright 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import java.util.*;

import org.jdom.Element;

import org.deltava.acars.message.*;
import org.deltava.acars.message.mp.MPUpdateMessage;

import org.deltava.util.*;

/**
 * An XML formatter for multi-player position update messages.
 * @author Luke
 * @version 2.2
 * @since 2.2
 */

public class MPUpdateFormatter extends ElementFormatter {

	/**
	 * Formats an MPUpdateMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	public Element format(Message msg) {
		
		// Cast the message
		MPUpdateMessage mpmsg = (MPUpdateMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "positions");
		for (Iterator<LocationMessage> i = mpmsg.getResponse().iterator(); i.hasNext(); ) {
			LocationMessage lmsg = i.next();
			
			// Build the element
			Element le = new Element("pos");
			le.setAttribute("lat", StringUtils.format(lmsg.getLatitude(), "##0.000000"));
			le.setAttribute("lon", StringUtils.format(lmsg.getLongitude(), "##0.000000"));
			le.setAttribute("flags", String.valueOf(lmsg.getFlags()));
			le.addContent(XMLUtils.createElement("h", String.valueOf(lmsg.getHeading())));
			le.addContent(XMLUtils.createElement("s", String.valueOf(lmsg.getAspeed())));
			le.addContent(XMLUtils.createElement("a", String.valueOf(lmsg.getAltitude())));
			le.addContent(XMLUtils.createElement("p", String.valueOf(lmsg.getPitch())));
			le.addContent(XMLUtils.createElement("b", String.valueOf(lmsg.getBank())));
			le.addContent(XMLUtils.createElement("fl", String.valueOf(lmsg.getFlaps())));
			le.addContent(XMLUtils.createElement("l", String.valueOf(lmsg.getLights())));
			e.addContent(le);
		}
		
		return pe;
	}
}