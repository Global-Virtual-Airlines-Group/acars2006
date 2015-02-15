// Copyright 2013, 2015 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

import java.util.Map;

import org.jdom2.Element;
import org.deltava.beans.acars.IATACodes;
import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.IATACodeMessage;
import org.deltava.util.XMLUtils;

/**
 * An XML Formatter for IATA code data messages.
 * @author Luke
 * @version 6.0
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
			for (Map.Entry<String, ? extends Number> me : fc.entrySet()) {
				Element ae = XMLUtils.createElement("code", me.getKey(), false);
				ae.setAttribute("count", String.valueOf(me.getValue()));
				fe.addContent(ae);
			}
			
			e.addContent(fe);
		}
		
		return pe;
	}
}