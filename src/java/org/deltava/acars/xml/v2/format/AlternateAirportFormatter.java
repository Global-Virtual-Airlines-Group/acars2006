// Copyright 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

import org.jdom2.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.AlternateAirportMessage;

import org.deltava.beans.schedule.Airport;

import org.deltava.util.XMLUtils;

/**
 * An XML formatter for AlternateAirportMessage beans.
 * @author Luke
 * @version 4.2
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
		for (Airport a : aamsg.getResponse()) {
			Element ae = XMLUtils.createElement("airport", a.getICAO());
			ae.setAttribute("iata", a.getIATA());
			e.addContent(ae);
		}
		
		return pe;
	}
}