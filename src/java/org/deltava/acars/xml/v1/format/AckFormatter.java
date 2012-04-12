// Copyright 2006, 2009, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import org.jdom2.Element;

import org.deltava.acars.message.*;

import org.deltava.util.XMLUtils;

/**
 * An XML Formatter for Acknowledge messages.
 * @author Luke
 * @version 4.2
 * @since 1.0
 */

class AckFormatter extends ElementFormatter {

	/**
	 * Formats an AcknowledgeMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {
		
		// Cast the message
		AcknowledgeMessage ackmsg = (AcknowledgeMessage) msg;

		// Create the element and the type
		Element e = initResponse(msg.getType());
		e.setAttribute("id", Long.toHexString(ackmsg.getParentID()));
		for (java.util.Map.Entry<String, String> me : ackmsg.getEntries())
			e.addContent(XMLUtils.createElement(me.getKey(), me.getValue(), true));

		// Return the element
		return e;
	}
}