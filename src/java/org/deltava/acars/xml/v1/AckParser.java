// Copyright 2004, 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1;

import org.jdom.Element;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.AcknowledgeMessage;
import org.deltava.acars.message.Message;
import org.deltava.acars.xml.XMLException;

/**
 * A parser for Acknowledge elements.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

class AckParser extends ElementParser {

	/**
	 * Convert an XML ack element into an AcknowledgeMessage.
	 * @param e the XML element
	 * @return an AcknowledgeMessage
	 * @throws XMLException if a parse error occurs 
	 */
	public Message parse(Element e, Pilot user) throws XMLException {
		
		// Get the id of the message we are acking
		String txtID = e.getAttributeValue("id");
		if (txtID == null)
			throw new XMLException("No Message ID to Acknowledge");

		// Create the bean and return it
		try {
			return new AcknowledgeMessage(user, Long.parseLong(txtID, 16));
		} catch (NumberFormatException nfe) {
			throw new XMLException("Cannot format ID " + txtID + " - " + nfe.getMessage());
		}
	}
}