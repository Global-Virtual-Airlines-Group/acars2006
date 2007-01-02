// Copyright 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1;

import java.util.Iterator;

import org.jdom.Element;

import org.deltava.acars.message.*;

import org.deltava.util.XMLUtils;

/**
 * An XML Formatter for System messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

class SystemMessageFormatter extends ElementFormatter {

	/**
	 * Formats a SystemTextMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
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