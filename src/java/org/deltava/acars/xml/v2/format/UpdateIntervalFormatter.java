// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

import org.deltava.acars.message.*;

import org.deltava.acars.xml.XMLElementFormatter;
import org.jdom.Element;

/**
 * An XML Formatter for position update interval messages.
 * @author Luke
 * @version 4.0
 * @since 4.0
 */

public class UpdateIntervalFormatter extends XMLElementFormatter {

	/**
	 * Formats an UpdateIntervalMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public org.jdom.Element format(Message msg) {
		
		// Cast the message
		UpdateIntervalMessage updmsg = (UpdateIntervalMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		pe.setAttribute("interval", String.valueOf(updmsg.getInterval()));
		return pe;
	}
}