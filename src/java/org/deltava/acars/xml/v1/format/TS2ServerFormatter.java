// Copyright 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import java.util.Iterator;

import org.jdom.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.TS2ServerMessage;

import org.deltava.beans.ts2.Server;

import org.deltava.util.XMLUtils;

/**
 * An XML Formatter for TeamSpeak 2 data messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

class TS2ServerFormatter extends ElementFormatter {

	/**
	 * Formats a TS2ServerMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	public Element format(Message msg) {
		
		// Cast the message
		TS2ServerMessage smsg = (TS2ServerMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "ts2servers");
		for (Iterator<Server> i = smsg.getResponse().iterator(); i.hasNext(); ) {
			Server srv = i.next();

			// Create the element
			Element srvE = new Element("ts2server");
			srvE.setAttribute("port", String.valueOf(srv.getPort()));
			srvE.setAttribute("name", srv.getName());
			srvE.addContent(XMLUtils.createElement("desc", srv.getDescription()));
			e.addContent(srvE);
		}
		
		return pe;
	}
}