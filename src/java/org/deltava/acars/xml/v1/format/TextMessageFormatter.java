// Copyright 2004, 2005, 2006, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import org.jdom.Element;

import org.deltava.acars.message.*;

import org.deltava.util.XMLUtils;

/**
 * An XML Formatter for Text messages.
 * @author Luke
 * @version 3.6
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
		if (msg.getSender() != null)
			e.addContent(XMLUtils.createElement("fromName", msg.getSender().getName()));
		
		e.addContent(XMLUtils.createElement("text", tmsg.getText()));
		if (!msg.isPublic())
			e.addContent(XMLUtils.createElement("to", tmsg.getRecipient()));

		return e;
	}
}