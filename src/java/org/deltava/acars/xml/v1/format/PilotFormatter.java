// Copyright 2006, 2007, 2008, 2010, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import java.util.Iterator;

import org.jdom2.Element;

import org.deltava.acars.message.*;
import org.deltava.acars.message.data.PilotMessage;

import org.deltava.beans.Pilot;
import org.deltava.util.StringUtils;
import org.deltava.util.XMLUtils;

/**
 * An XML Formatter for Pilot data messages.
 * @author Luke
 * @version 4.2
 * @since 1.0
 */

class PilotFormatter extends ElementFormatter {

	/**
	 * Formats a PilotMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
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
			ue.setAttribute("dbID", p.getHexID());
			ue.addContent(XMLUtils.createElement("firstname", p.getFirstName()));
			ue.addContent(XMLUtils.createElement("lastname", p.getLastName()));
			ue.addContent(XMLUtils.createElement("name", p.getName()));
			ue.addContent(XMLUtils.createElement("eqtype", p.getEquipmentType()));
			ue.addContent(XMLUtils.createElement("rank", p.getRank().getName()));
			ue.addContent(XMLUtils.createElement("hours", String.valueOf(p.getHours())));
			ue.addContent(XMLUtils.createElement("legs", String.valueOf(p.getLegs())));
			ue.addContent(XMLUtils.createElement("joinedOn", StringUtils.format(p.getCreatedOn(), "MMMM dd, yyyy")));
			ue.addContent(XMLUtils.createElement("roles", StringUtils.listConcat(p.getRoles(), ",")));
			ue.addContent(XMLUtils.createElement("ratings", StringUtils.listConcat(p.getRatings(), ",")));
			ue.addContent(XMLUtils.createElement("isDispatch", String.valueOf(pmsg.isDispatch())));
			e.addContent(ue);
		}
		
		return pe;
	}
}