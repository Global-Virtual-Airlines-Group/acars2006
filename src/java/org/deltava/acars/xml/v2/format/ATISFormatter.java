// Copyright 2022 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

import org.jdom2.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.ATISMessage;

import org.deltava.beans.navdata.ATIS;
import org.deltava.util.XMLUtils;

/**
 * An XML formatter for ATISMessage beans. 
 * @author Luke
 * @version 10.3
 * @since 10.3
 */

class ATISFormatter extends ElementFormatter {

	/**
	 * Formats an ATISMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {
		
		// Cast the message
		ATISMessage amsg = (ATISMessage) msg;
		
		// Build the DataResponseElement
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "atis");
		for (ATIS a : amsg.getResponse()) {
			Element ae = new Element("atis");
			ae.setAttribute("type", a.getType().getDescription());
			ae.setAttribute("code", String.valueOf(a.getCode()));
			ae.setAttribute("effectiveDate", String.valueOf(a.getEffectiveDate().toEpochMilli() / 1000));
			ae.addContent(formatAirport(a.getAirport(), "airport"));
			ae.addContent(XMLUtils.createElement("data", a.getData(), true));
			e.addContent(ae);
		}

		return pe;
	}
}