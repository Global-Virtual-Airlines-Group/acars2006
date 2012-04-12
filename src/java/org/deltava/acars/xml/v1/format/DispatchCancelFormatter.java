// Copyright 2007, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import org.jdom2.Element;

import org.deltava.acars.message.*;
import org.deltava.acars.message.dispatch.CancelMessage;

import org.deltava.util.XMLUtils;

/**
 * An XML formatter for DispatchCancel messages.
 * @author Luke
 * @version 4.2
 * @since 2.0
 */

public class DispatchCancelFormatter extends ElementFormatter {

	/**
	 * Formats a DispatchCancelMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {
		
		// Cast the message
		CancelMessage cmsg = (CancelMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, cmsg.getRequestTypeName());
		e.addContent(XMLUtils.createElement("originator", msg.getSenderID()));
		return pe;
	}
}