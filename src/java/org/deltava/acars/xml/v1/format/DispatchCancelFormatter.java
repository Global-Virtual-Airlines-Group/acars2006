// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import org.jdom.Element;

import org.deltava.acars.message.*;
import org.deltava.acars.message.dispatch.CancelMessage;

import org.deltava.util.XMLUtils;

/**
 * An XML formatter for DispatchCancel messages.
 * @author Luke
 * @version 2.0
 * @since 2.0
 */

public class DispatchCancelFormatter extends ElementFormatter {

	/**
	 * Formats a DispatchCancelMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
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