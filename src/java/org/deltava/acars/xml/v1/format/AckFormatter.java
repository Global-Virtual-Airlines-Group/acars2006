// Copyright 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import java.util.Iterator;

import org.jdom.Element;

import org.deltava.acars.message.*;

import org.deltava.util.XMLUtils;

/**
 * An XML Formatter for Acknowledge messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

class AckFormatter extends ElementFormatter {

	/**
	 * Formats an AcknowledgeMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	public Element format(Message msg) {
		
		// Cast the message
		AcknowledgeMessage ackmsg = (AcknowledgeMessage) msg;

		// Create the element and the type
		Element e = initResponse(msg.getType());
		e.setAttribute("id", Long.toHexString(ackmsg.getParentID()).toUpperCase());

		// Display additional elements
		for (Iterator<String> i = ackmsg.getEntryNames().iterator(); i.hasNext();) {
			String eName = i.next();
			e.addContent(XMLUtils.createElement(eName, ackmsg.getEntry(eName), true));
		}

		// Return the element
		return e;
	}
}