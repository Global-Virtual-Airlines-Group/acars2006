// Copyright 2013 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

import org.jdom2.Element;

import org.deltava.beans.acars.IATACodes;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.IATACodeMessage;

import org.deltava.util.XMLUtils;

/**
 * An XML Formatter for IATA code data messages.
 * @author Luke
 * @version 5.1
 * @since 5.1
 */

class IATACodeFormatter extends org.deltava.acars.xml.XMLElementFormatter {

	/**
	 * Formats an FDEMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {
		
		// Cast the message
		IATACodeMessage fmsg = (IATACodeMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "iatacodes");
		for (IATACodes fc : fmsg.getResponse()) {
			Element fe = new Element("eqType");
			fe.setAttribute("type", fc.getEquipmentType());
			for (String code : fc.keySet())
				fe.addContent(XMLUtils.createElement("code", code, false));
			
			e.addContent(fe);
		}
		
		return pe;
	}
}