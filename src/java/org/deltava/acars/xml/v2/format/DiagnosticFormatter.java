// Copyright 2017, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

import org.jdom2.Element;

import org.deltava.acars.message.*;

/**
 * An XML formatter for diagnostic messages.
 * @author Luke
 * @version 8.4
 * @since 7.3
 */

public class DiagnosticFormatter extends ElementFormatter {

	/**
	 * Formats a DiagnosticMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {
		
		// Cast the message
		DiagnosticMessage dmsg = (DiagnosticMessage) msg;
		
		// Build the ResponseElement
		Element pe = initResponse(msg.getType());
		pe.setAttribute("autoReconnect", String.valueOf(dmsg.getRequestType() != DiagRequest.WARN));
		return pe;
	}
}