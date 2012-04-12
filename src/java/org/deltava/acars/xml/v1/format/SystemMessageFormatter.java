// Copyright 2005, 2006, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import java.util.Iterator;

import org.jdom2.Element;

import org.deltava.acars.message.*;

import org.deltava.util.XMLUtils;

/**
 * An XML Formatter for System messages.
 * @author Luke
 * @version 4.2
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
		e.setAttribute("msgtype", "text");
		e.addContent(XMLUtils.createElement("time", Long.toHexString(msg.getTime())));
		for (Iterator<String> i = smsg.getMsgs().iterator(); i.hasNext();) {
			String msgText = i.next();
			e.addContent(XMLUtils.createElement("text", msgText, true));
		}

		return e;
	}
}