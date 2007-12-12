// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import org.jdom.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.dispatch.CompleteMessage;

import org.deltava.util.XMLUtils;

/**
 * An XML formatter for DispatchComplete messages.
 * @author Luke
 * @version 2.0
 * @since 2.0
 */

public class ServiceCompleteFormatter extends ElementFormatter {

	/**
	 * Formats a DispatchCompleteMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	public Element format(Message msg) {
		
		// Cast the message
		CompleteMessage cmsg = (CompleteMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, cmsg.getRequestTypeName());
		e.addContent(XMLUtils.createElement("originator", msg.getSenderID()));
		return pe;
	}
}