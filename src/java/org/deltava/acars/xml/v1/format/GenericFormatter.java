// Copyright 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import java.util.Iterator;

import org.jdom.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.GenericMessage;

import org.deltava.util.XMLUtils;

/**
 * An XML Formatter for Generic data messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

class GenericFormatter extends ElementFormatter {

	/**
	 * Formats a GenericMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	public Element format(Message msg) {

		// Cast the message
		GenericMessage gmsg = (GenericMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "info");
		for (Iterator<String> i = gmsg.getResponse().iterator(); i.hasNext(); ) {
			String value = i.next();
			e.addContent(XMLUtils.createElement(gmsg.getLabel(), value));
		}
		
		return pe;
	}
}