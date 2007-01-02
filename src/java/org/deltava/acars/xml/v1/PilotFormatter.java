// Copyright 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1;

import java.util.Iterator;

import org.jdom.Element;

import org.deltava.acars.message.*;
import org.deltava.acars.message.data.PilotMessage;

import org.deltava.beans.Pilot;
import org.deltava.util.StringUtils;
import org.deltava.util.XMLUtils;

/**
 * An XML Formatter for Pilot data messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

class PilotFormatter extends ElementFormatter {

	/**
	 * Formats a PilotMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	public Element format(Message msg) {
		
		// Cast the message
		PilotMessage pmsg = (PilotMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, (pmsg.getRequestType() == DataMessage.REQ_ADDUSER) ? "addpilots" : "delpilots");
		for (Iterator<Pilot> i = pmsg.getResponse().iterator(); i.hasNext(); ) {
			Pilot p = i.next();
			
			// Create the elemnet
			Element ue = new Element("Pilot");
			ue.setAttribute("id", p.getPilotCode());
			ue.addContent(XMLUtils.createElement("firstname", p.getFirstName()));
			ue.addContent(XMLUtils.createElement("lastname", p.getLastName()));
			ue.addContent(XMLUtils.createElement("name", p.getName()));
			ue.addContent(XMLUtils.createElement("eqtype", p.getEquipmentType()));
			ue.addContent(XMLUtils.createElement("rank", p.getRank()));
			ue.addContent(XMLUtils.createElement("hours", String.valueOf(p.getHours())));
			ue.addContent(XMLUtils.createElement("legs", String.valueOf(p.getLegs())));
			ue.addContent(XMLUtils.createElement("joinedOn", StringUtils.format(p.getCreatedOn(), "MMMM dd, yyyy")));
			ue.addContent(XMLUtils.createElement("roles", StringUtils.listConcat(p.getRoles(), ",")));
			e.addContent(ue);
		}
		
		return pe;
	}
}