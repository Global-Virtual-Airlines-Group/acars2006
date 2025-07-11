// Copyright 2010, 2012, 2018, 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

import org.jdom2.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.dispatch.RoutePlotMessage;
import org.deltava.util.XMLUtils;

/**
 * An XML formatter for route plot messages.
 * @author Luke
 * @version 10.0
 * @since 3.0
 */

public class RoutePlotFormatter extends ElementFormatter {

	/**
	 * Formats a RoutePlotMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {
		
		// Cast the message
		RoutePlotMessage rmsg = (RoutePlotMessage) msg;
		
		// Build the DispatchResponseElement
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, rmsg.getRequestType().getCode());
		e.setAttribute("id", String.valueOf(rmsg.getID()));
		XMLUtils.addIfPresent(e, formatRoute(rmsg.getResults()));
		return pe;
	}
}