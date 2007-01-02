// Copyright 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1;

import org.jdom.Element;

import org.deltava.acars.message.*;

import org.deltava.util.XMLUtils;

/**
 * An XML Formatter for Diagnostic messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

class DiagnosticFormatter extends ElementFormatter {

	/**
	 * Formats a DiagnosticMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	public Element format(Message msg) {

		// Cast the message 
		DiagnosticMessage dmsg = (DiagnosticMessage) msg;
		
		// Create the element and the type
		Element e = initResponse(msg.getType());

		// Save the type
		e.addContent(XMLUtils.createElement("reqtype", Message.MSG_TYPES[dmsg.getRequestType()]));
		e.addContent(XMLUtils.createElement("reqData", dmsg.getRequestData()));
		e.addContent(XMLUtils.createElement("time", Long.toHexString(msg.getTime())));

		// Return the element
		return e;
	}
}