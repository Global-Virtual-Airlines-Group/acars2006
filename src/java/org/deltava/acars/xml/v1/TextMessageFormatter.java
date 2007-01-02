// Copyright 2004, 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1;

import org.jdom.Element;

import org.deltava.acars.message.*;

import org.deltava.util.XMLUtils;

/**
 * An XML Formatter for Text messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

class TextMessageFormatter extends ElementFormatter {

	/**
	 * Formats a TextMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	public Element format(Message msg) {
		
		// Cast the message
		TextMessage tmsg = (TextMessage) msg;

		// Create the element
		Element e = initResponse(msg.getType());
		e.addContent(XMLUtils.createElement("from", msg.getSenderID()));
		e.addContent(XMLUtils.createElement("text", tmsg.getText()));
		e.addContent(XMLUtils.createElement("time", Long.toHexString(msg.getTime())));
		if (!msg.isPublic())
			e.addContent(XMLUtils.createElement("to", tmsg.getRecipient()));

		return e;
	}
}