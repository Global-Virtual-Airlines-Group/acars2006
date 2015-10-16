// Copyright 2011, 2012, 2015 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

import org.jdom2.Element;

import org.deltava.acars.message.*;

import org.deltava.acars.xml.XMLElementFormatter;

/**
 * An XML Formatter for position update interval messages.
 * @author Luke
 * @version 6.2
 * @since 4.0
 */

public class UpdateIntervalFormatter extends XMLElementFormatter {

	/**
	 * Formats an UpdateIntervalMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {
		
		// Cast the message
		IntervalMessage updmsg = (IntervalMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		pe.setAttribute("interval", String.valueOf(updmsg.getInterval()));
		return pe;
	}
}