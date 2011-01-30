// Copyright 2010, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

import org.jdom.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.AppInfoMessage;

import org.deltava.beans.system.AirlineInformation;

import org.deltava.util.XMLUtils;

/**
 * An XML formatter for Cross-Application information messages.
 * @author Luke
 * @version 3.6
 * @since 3.6
 */

public class AppInfoFormatter extends ElementFormatter {

	/**
	 * Formats an AppInfoMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {
		
		// Cast the message
		AppInfoMessage imsg = (AppInfoMessage) msg;
		
		// Build the DispatchResponseElement
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "appinfo");
		for (AirlineInformation ai : imsg.getResponse()) {
			Element ae = XMLUtils.createElement("app", ai.getName(), true);
			ae.setAttribute("code", ai.getCode());
			ae.setAttribute("domain", ai.getDomain());
			e.addContent(ae);
		}
		
		return pe;
	}
}