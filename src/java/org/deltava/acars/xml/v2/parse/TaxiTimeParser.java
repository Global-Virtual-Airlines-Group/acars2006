// Copyright 2024 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import org.jdom2.Element;

import org.deltava.acars.message.TaxiTimeMessage;

import org.deltava.acars.xml.*;

import org.deltava.beans.Pilot;
import org.deltava.util.StringUtils;


/**
 * A parser for ACARS taxi time messages.
 * @author Luke
 * @version 11.2
 * @since 11.2
 */

public class TaxiTimeParser extends XMLElementParser<TaxiTimeMessage> {

	/**
	 * Convert an XML taxitimes element into a TaxiTimeMessage.
	 * @param e the XML element
	 * @return a TaxiTimeMessage
	 * @throws XMLException if a parse error occurs 
	 */
	@Override
	public TaxiTimeMessage parse(Element e, Pilot usr) throws XMLException {
		
		// Create the message
		TaxiTimeMessage msg = new TaxiTimeMessage(usr);
		msg.setFlightID(StringUtils.parse(e.getAttributeValue("id"), 0));
		msg.setInboundTaxiTime(StringUtils.parse(getChildText(e, "taxiIn", "0"), 0));
		msg.setOutboundTaxiTime(StringUtils.parse(getChildText(e, "taxiOut", "0"), 0));
		return msg;
	}
}