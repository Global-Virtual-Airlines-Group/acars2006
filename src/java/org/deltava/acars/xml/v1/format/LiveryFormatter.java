// Copyright 2008, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import java.util.Iterator;

import org.jdom2.Element;

import org.deltava.beans.acars.Livery;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.LiveryMessage;

import org.deltava.util.XMLUtils;

/**
 * An XML formatter for multi-player livery data messages.
 * @author Luke
 * @version 4.2
 * @since 2.2
 */

public class LiveryFormatter extends ElementFormatter {

	/**
	 * Formats a LiveryMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {
		
		// Cast the message
		LiveryMessage lmsg = (LiveryMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "liveries");
		for (Iterator<Livery> i = lmsg.getResponse().iterator(); i.hasNext(); ) {
			Livery l  = i.next();
			Element le = XMLUtils.createElement("livery", l.getDescription(), true);
			le.setAttribute("airline", l.getAirline().getCode());
			le.setAttribute("code", l.getCode());
			if (l.getDefault())
				le.setAttribute("default", "true");
			
			e.addContent(le);
		}
		
		return pe;
	}
}