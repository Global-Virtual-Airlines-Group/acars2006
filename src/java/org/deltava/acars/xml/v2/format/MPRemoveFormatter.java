// Copyright 2011, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

import org.jdom2.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.mp.RemoveMessage;

import org.deltava.acars.xml.XMLElementFormatter;

import org.deltava.beans.Pilot;

/**
 * An XML formatter for multi-player position update messages.
 * @author Luke
 * @version 4.2
 * @since 4.1
 */

public class MPRemoveFormatter extends XMLElementFormatter {

	/**
	 * Formats a RemoveMessage bean into an XML element.
	 * @param msg the RemoveMessage
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {
		
		// Get the message
		RemoveMessage rmsg = (RemoveMessage) msg;
		
		// Create the element
		Pilot p = msg.getSender();
		Element pe = initResponse(msg.getType());
		pe.setAttribute("id", p.getHexID());
		pe.setAttribute("flightID", Integer.toHexString(rmsg.getFlightID()));
		return pe;
	}
}