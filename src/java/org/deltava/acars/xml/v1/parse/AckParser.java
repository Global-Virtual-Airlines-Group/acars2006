// Copyright 2004, 2005, 2006, 2007, 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import org.jdom.Element;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.AcknowledgeMessage;
import org.deltava.acars.xml.XMLElementParser;
import org.deltava.acars.xml.XMLException;

/**
 * A parser for Acknowledge elements.
 * @author Luke
 * @version 2.8
 * @since 1.0
 */

class AckParser extends XMLElementParser<AcknowledgeMessage> {

	/**
	 * Convert an XML ack element into an AcknowledgeMessage.
	 * @param e the XML element
	 * @return an AcknowledgeMessage
	 * @throws XMLException if a parse error occurs 
	 */
	public AcknowledgeMessage parse(Element e, Pilot user) throws XMLException {
		
		// Get the id of the message we are acking
		String txtID = e.getAttributeValue("id");
		if (txtID == null)
			return null;

		// Create the bean and return it
		try {
			return new AcknowledgeMessage(user, Long.parseLong(txtID, 16));
		} catch (NumberFormatException nfe) {
			throw new XMLException("Cannot format ID " + txtID + " - " + nfe.getMessage());
		}
	}
}