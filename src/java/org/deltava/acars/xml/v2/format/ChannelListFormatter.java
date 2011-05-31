// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

import java.util.Iterator;

import org.jdom.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.ChannelListMessage;

import org.deltava.acars.xml.XMLElementFormatter;

import org.deltava.beans.Pilot;
import org.deltava.beans.mvs.*;

import org.deltava.util.*;

/**
 * An XML formatter for Voice Channel data messages.
 * @author Luke
 * @version 4.0
 * @since 4.0
 */

class ChannelListFormatter extends XMLElementFormatter {

	/**
	 * Formats a ChannelMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {
		
		// Cast the message
		ChannelListMessage cmsg = (ChannelListMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "channels");
		for (Iterator<PopulatedChannel> i = cmsg.getResponse().iterator(); i.hasNext(); ) {
			PopulatedChannel pc = i.next();
			Channel c = pc.getChannel();
			
			// Build the channel element
			Element ce = new Element("channel");
			ce.setAttribute("id", c.getHexID());
			ce.setAttribute("rate", String.valueOf(c.getSampleRate().getRate()));
			ce.setAttribute("lat", StringUtils.format(c.getCenter().getLatitude(), "#0.0000"));
			ce.setAttribute("lng", StringUtils.format(c.getCenter().getLongitude(), "##0.0000"));
			ce.setAttribute("range", String.valueOf(c.getRange()));
			ce.addContent(XMLUtils.createElement("name", c.getName(), true));
			
			// Format users
			for (Pilot p : pc.getUsers()) {
				Element ue = new Element("user");
				ue.setAttribute("id", p.getPilotCode());
				ue.setAttribute("dbID", p.getHexID());
				ue.setAttribute("owner", String.valueOf(c.getIsTemporary() && (c.getOwner().getID() == p.getID())));
				ue.addContent(XMLUtils.createElement("firstname", p.getFirstName()));
				ue.addContent(XMLUtils.createElement("lastname", p.getLastName()));
				ue.addContent(XMLUtils.createElement("name", p.getName()));
				ue.addContent(XMLUtils.createElement("eqtype", p.getEquipmentType()));
				ue.addContent(XMLUtils.createElement("rank", p.getRank().getName()));
				ce.addContent(ue);
			}
			
			e.addContent(ce);
		}
		
		return pe;
	}
}