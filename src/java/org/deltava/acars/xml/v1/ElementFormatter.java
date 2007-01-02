// Copyright 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1;

import org.jdom.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.xml.ProtocolInfo;

import org.deltava.beans.schedule.Airport;

import org.deltava.util.StringUtils;
import org.deltava.util.XMLUtils;

/**
 * A formatter to create XML command elements.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

abstract class ElementFormatter {

	/**
	 * Formats a Message bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	public abstract Element format(Message msg);
	
	/**
	 * Helper method to format an Airport bean.
	 */
	protected Element formatAirport(Airport a, String eName) {
		Element ae = new Element(eName);
		if (a != null) {
			ae.setAttribute("name", a.getName());
			ae.setAttribute("icao", a.getICAO());
			ae.setAttribute("iata", a.getIATA());
			ae.setAttribute("lat", StringUtils.format(a.getLatitude(), "##0.0000"));
			ae.setAttribute("lng", StringUtils.format(a.getLongitude(), "##0.0000"));
		}

		return ae;
	}
	
	/**
	 * Helper method to initialize the response element.
	 */
	protected Element initResponse(int msgType) {
		Element e = new Element(ProtocolInfo.CMD_ELEMENT_NAME);
		e.setAttribute("type", Message.MSG_CODES[msgType]);
		return e;
	}
	
	/**
	 * Helper method to initialize a DataResponse element.
	 */
	protected Element initDataResponse(Element cmd, String rspType) {
		// Get the element
		Element e = cmd.getChild(rspType);
		if (e != null)
			return e;

		// Create the new element
		cmd.addContent(XMLUtils.createElement("rsptype", rspType));
		e = new Element(rspType);
		cmd.addContent(e);
		return e;
	}
}