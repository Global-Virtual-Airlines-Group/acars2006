// Copyright 2006, 2012, 2018, 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import org.jdom2.Element;

import org.deltava.acars.message.*;

import org.deltava.util.XMLUtils;

/**
 * An XML Formatter for Diagnostic messages.
 * @author Luke
 * @version 9.1
 * @since 1.0
 */

class DiagnosticFormatter extends ElementFormatter {

	/**
	 * Formats a DiagnosticMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {

		// Cast the message 
		DiagnosticMessage dmsg = (DiagnosticMessage) msg;
		
		// Create the element and the type
		Element e = initResponse(msg.getType());
		e.addContent(XMLUtils.createElement("reqtype", dmsg.getRequestType().getCode()));
		e.addContent(XMLUtils.createElement("reqData", dmsg.getRequestData()));
		e.addContent(XMLUtils.createElement("time", Long.toHexString(msg.getTime())));
		return e;
	}
}