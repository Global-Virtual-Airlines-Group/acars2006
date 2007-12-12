// Copyright 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.*;
import org.deltava.acars.xml.XMLException;

/**
 * A Parser for Diagnostic elements.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

class DiagnosticParser extends ElementParser {

	/**
	 * Convert an XML diag element into a DiagnosticMessage.
	 * @param e the XML element
	 * @return a DiagnosticMessage
	 * @throws XMLException if a parse error occurs 
	 */
	public Message parse(org.jdom.Element e, Pilot user) throws XMLException {

		// Create the message
		DiagnosticMessage msg = new DiagnosticMessage(user);
		msg.setRequestType(getChildText(e, "reqtype", ""));
		msg.setRequestData(getChildText(e, "reqdata", null));
		return msg;
	}
}