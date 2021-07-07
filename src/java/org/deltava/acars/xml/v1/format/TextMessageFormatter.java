// Copyright 2004, 2005, 2006, 2011, 2012, 2019, 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import org.jdom2.Element;

import org.deltava.acars.message.*;

import org.deltava.util.XMLUtils;

/**
 * An XML Formatter for Text messages.
 * @author Luke
 * @version 10.1
 * @since 1.0
 */

class TextMessageFormatter extends ElementFormatter {

	/**
	 * Formats a TextMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {
		
		TextMessage tmsg = (TextMessage) msg;

		// Create the element
		Element e = initResponse(msg.getType());
		e.addContent(XMLUtils.createElement("from", msg.getSenderID()));
		if (msg.getSender() != null) {
			e.addContent(XMLUtils.createElement("fromID", String.valueOf(msg.getSender().getID())));
			e.addContent(XMLUtils.createElement("fromName", msg.getSender().getName()));
		}
		
		e.addContent(XMLUtils.createElement("text", tmsg.getText()));
		XMLUtils.addIfPresent(e, XMLUtils.createIfPresent("channel", tmsg.getChannel()));
		XMLUtils.addIfPresent(e, XMLUtils.createIfPresent("to", tmsg.getRecipient()));
		return e;
	}
}