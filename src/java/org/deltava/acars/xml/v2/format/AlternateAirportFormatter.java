// Copyright 2012, 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

import org.jdom2.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.AlternateAirportMessage;

/**
 * An XML formatter for AlternateAirportMessage beans.
 * @author Luke
 * @version 10.0
 * @since 4.2
 */

public class AlternateAirportFormatter extends ElementFormatter {

	/**
	 * Formats an AlternateAirportMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {
		
		// Cast the message
		AlternateAirportMessage aamsg = (AlternateAirportMessage) msg;
		
		// Build the DataResponseElement
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "alternate");
		aamsg.getResponse().forEach(a -> e.addContent(formatAirport(a, "airport")));
		return pe;
	}
}