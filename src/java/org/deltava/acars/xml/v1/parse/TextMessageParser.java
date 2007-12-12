// Copyright 2004, 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.TextMessage;
import org.deltava.acars.xml.XMLException;
import org.deltava.beans.Pilot;
import org.jdom.Element;

/**
 * A Parser for Text Message elements.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

class TextMessageParser extends ElementParser {

	/**
	 * Convert an XML chat element into a TextMessage.
	 * @param e the XML element
	 * @return a TextMessage
	 * @throws XMLException if a parse error occurs 
	 */
	public Message parse(Element e, Pilot user) throws XMLException {

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