// Copyright 2010, 2011, 2012, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import org.jdom2.Element;

import org.deltava.acars.xml.XMLElementParser;

import org.deltava.acars.message.dispatch.ScopeInfoMessage;

import org.deltava.beans.*;
import org.deltava.beans.schedule.GeoPosition;
import org.deltava.util.*;

/**
 * An XML Parser for radar scope information messages. 
 * @author Luke
 * @version 7.2
 * @since 3.0
 */

class ScopeInfoParser extends XMLElementParser<ScopeInfoMessage> {
	
	/**
	 * Convert an XML scope info element into a ScopeInfoMessage.
	 * @param e the XML element
	 * @return a ScopeInfoMessage
	 */
	@Override
	public ScopeInfoMessage parse(Element e, Pilot usr) {
		
		// Create the location
		GeoLocation loc = new GeoPosition(StringUtils.parse(getChildText(e, "lat", "0"), 0.0d), StringUtils.parse(getChildText(e, "lng", "0"), 0.0d));
		
		// Create the message
		ScopeInfoMessage msg = new ScopeInfoMessage(usr);
		msg.setCenter(loc);
		msg.setRange(StringUtils.parse(getChildText(e, "range", "0"), 0));
		msg.setFrequency(getChildText(e, "freq", null));
		msg.setCallsign(getChildText(e, "callsign", null));
		String network = getChildText(e, "network", "");
		if ("ALL".equals(network))
			msg.setAllTraffic(true);
		else if (network.length() > 0)
			msg.setNetwork(OnlineNetwork.fromName(network));
		
		return msg;
	}
}