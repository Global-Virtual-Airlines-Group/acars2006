// Copyright 2005, 2006, 2012, 2017 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import org.jdom2.Element;

import org.deltava.acars.message.*;

import org.deltava.util.XMLUtils;

/**
 * An XML Formatter for System messages.
 * @author Luke
 * @version 7.3
 * @since 1.0
 */

class SystemMessageFormatter extends ElementFormatter {

	/**
	 * Formats a SystemTextMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {
	
		// Cast the message
		SystemTextMessage smsg = (SystemTextMessage) msg;
		
		// Create the element 
		Element e = initResponse(msg.getType());
		e.setAttribute("msgtype", smsg.isWarning() ? "warning" : "text");
		e.addContent(XMLUtils.createElement("time", Long.toHexString(msg.getTime())));
		smsg.getMessages().forEach(msgText -> e.addContent(XMLUtils.createElement("text", msgText, true)));
		return e;
	}
}