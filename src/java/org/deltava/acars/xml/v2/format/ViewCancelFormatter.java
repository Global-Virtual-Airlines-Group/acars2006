// Copyright 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

import org.jdom.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.viewer.CancelMessage;

import org.deltava.acars.xml.XMLElementFormatter;

import org.deltava.util.XMLUtils;

/**
 * A formatter for Flight Viewer cancelation messages.
 * @author Luke
 * @version 2.8
 * @since 2.8
 */

public class ViewCancelFormatter extends XMLElementFormatter {

	/**
	 * Formats a ViewRequestMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {
		
		// Cast the message
		CancelMessage cmsg = (CancelMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		initDataResponse(pe, cmsg.getRequestTypeName());
		pe.addContent(XMLUtils.createElement("originator", msg.getSenderID()));
		return pe;
	}
}