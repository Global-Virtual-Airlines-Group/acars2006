// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

import org.jdom.Element;

import org.deltava.acars.message.*;

import org.deltava.acars.xml.XMLElementFormatter;

import org.deltava.beans.Pilot;

/**
 * An XML formatter for multi-player position update messages.
 * @author Luke
 * @version 4.1
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
		
		// Create the element
		Pilot p = msg.getSender();
		Element pe = initResponse(msg.getType());
		pe.setAttribute("id", p.getPilotCode());
		pe.setAttribute("dbID", Integer.toHexString(p.getID()));
		return pe;
	}
}