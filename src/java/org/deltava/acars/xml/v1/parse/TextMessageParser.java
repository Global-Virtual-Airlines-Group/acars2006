// Copyright 2004, 2005, 2006, 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import org.jdom.Element;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.TextMessage;
import org.deltava.acars.xml.XMLException;

/**
 * A Parser for Text Message elements.
 * @author Luke
 * @version 2.2
 * @since 1.0
 */

class TextMessageParser extends ElementParser<TextMessage> {

	/**
	 * Convert an XML chat element into a TextMessage.
	 * @param e the XML element
	 * @return a TextMessage
	 * @throws XMLException if a parse error occurs 
	 */
	public TextMessage parse(Element e, Pilot user) throws XMLException {

		// Get the message text
		String msgText = getChildText(e, "text", null);
		if (msgText == null)
			throw new XMLException("No Message Text");

		// Create the bean and set the receipients
		TextMessage msg = new TextMessage(user, msgText);
		msg.setRecipient(getChildText(e, "to", null));

		// Return the message bean
		return msg;
	}
}