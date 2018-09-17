// Copyright 2007, 2012, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import org.jdom2.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.dispatch.CompleteMessage;

import org.deltava.util.XMLUtils;

/**
 * An XML formatter for DispatchComplete messages.
 * @author Luke
 * @version 8.4
 * @since 2.0
 */

public class ServiceCompleteFormatter extends ElementFormatter {

	/**
	 * Formats a DispatchCompleteMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {
		
		// Cast the message
		CompleteMessage cmsg = (CompleteMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, cmsg.getRequestType().getCode());
		e.addContent(XMLUtils.createElement("originator", msg.getSenderID()));
		return pe;
	}
}