// Copyright 2010 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

import org.jdom.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.dispatch.RoutePlotMessage;

/**
 * An XML formatter for route plot messages.
 * @author Luke
 * @version 3.0
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
		if (rmsg.getResults() != null) {
			Element re = formatRoute(rmsg.getResults());
			pe.addContent(re);
		}
		
		return pe;
	}
}