// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

import java.util.*;

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
		e.setAttribute("clear", String.valueOf(cmsg.getClearList()));
		for (Iterator<PopulatedChannel> i = cmsg.getResponse().iterator(); i.hasNext(); ) {
			PopulatedChannel pc = i.next();
			Channel c = pc.getChannel();
			
			// Build the channel element
			Element ce = new Element("channel");
			ce.setAttribute("id", String.valueOf(c.getID()));
			ce.setAttribute("rate", String.valueOf(c.getSampleRate().getRate()));
			ce.setAttribute("range", String.valueOf(c.getRange()));
			ce.setAttribute("users", String.valueOf(c.getMaxUsers()));
			ce.setAttribute("default", String.valueOf(c.getIsDefault()));
			ce.addContent(XMLUtils.createElement("name", c.getName(), true));
			ce.addContent(XMLUtils.createElement("desc", c.getDescription(), true));
			if (!StringUtils.isEmpty(c.getFrequency()))
				ce.addContent(XMLUtils.createElement("freq", c.getFrequency()));
			if (c.getCenter() != null) {
				ce.addContent(XMLUtils.createElement("lat", StringUtils.format(c.getCenter().getLatitude(), "#0.0000")));
				ce.addContent(XMLUtils.createElement("lng", StringUtils.format(c.getCenter().getLongitude(), "##0.0000")));
			}
			
			// Display channel roles
			ce.addContent(formatRoles(c.getTalkRoles(), "talk"));
			ce.addContent(formatRoles(c.getAdminRoles(), "admin"));
				
			// Format users
			for (Pilot p : pc.getUsers()) {
				Element ue = new Element("user");
				ue.setAttribute("id", p.getPilotCode());
				ue.setAttribute("dbID", p.getHexID().substring(2));
				ue.setAttribute("owner", String.valueOf(c.getIsTemporary() && (c.getOwner().getID() == p.getID())));
				ue.addContent(XMLUtils.createElement("name", p.getName(), true));
				ue.addContent(XMLUtils.createElement("eqtype", p.getEquipmentType()));
				ue.addContent(XMLUtils.createElement("rank", p.getRank().getName()));
				ce.addContent(ue);
			}
			
			e.addContent(ce);
		}
		
		return pe;
	}
	
	/**
	 * Helper method to format role names.
	 */
	private Collection<Element> formatRoles(Collection<String> roleNames, String type) {
		Collection<Element> results = new ArrayList<Element>();
		for (String name : roleNames) {
			Element re = XMLUtils.createElement("role", name);
			re.setAttribute("type", type);
			results.add(re);
		}
		
		return results;
	}
}