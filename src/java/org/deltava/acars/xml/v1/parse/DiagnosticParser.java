// Copyright 2005, 2006, 2008, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.*;
import org.deltava.acars.xml.XMLElementParser;

/**
 * A Parser for Diagnostic message elements.
 * @author Luke
 * @version 2.8
 * @since 1.0
 */

class DiagnosticParser extends XMLElementParser<DiagnosticMessage> {

	/**
	 * Convert an XML diag element into a DiagnosticMessage.
	 * @param e the XML element
	 * @return a DiagnosticMessage
	 */
	public DiagnosticMessage parse(org.jdom.Element e, Pilot user) {

		// Create the message
		DiagnosticMessage msg = new DiagnosticMessage(user);
		msg.setRequestType(getChildText(e, "reqtype", ""));
		msg.setRequestData(getChildText(e, "reqdata", null));
		return msg;
	}
}