// Copyright 2008, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import org.jdom.Element;

import org.deltava.beans.Pilot;
import org.deltava.beans.schedule.*;

import org.deltava.acars.message.dispatch.RangeMessage;
import org.deltava.acars.xml.XMLElementParser;

import org.deltava.util.StringUtils;
import org.deltava.util.system.SystemData;

/**
 * A parser for Dispatch service range elements.
 * @author Luke
 * @version 2.8
 * @since 2.2
 */

public class DispatchRangeParser extends XMLElementParser<RangeMessage> {

	/**
	 * Convert an XML dispatcher range element into a RangeMessage.
	 * @param e the XML element
	 * @param user the originating user
	 * @return a RangeMessage
	 */
	public RangeMessage parse(Element e, Pilot user) {
		
		// Create the and set range
		RangeMessage msg = new RangeMessage(user);
		msg.setRange(StringUtils.parse(getChildText(e, "range", null), Integer.MAX_VALUE));
		
		// Check for an airport
		Airport a = SystemData.getAirport(getChildText(e, "airport", null));
		if (a == null) {
			double lat = StringUtils.parse(getChildText(e, "lat", "0.0"), 0.0d);
			double lng = StringUtils.parse(getChildText(e, "lng", "0.0"), 0.0d);
			msg.setLocation(new GeoPosition(lat, lng));
		} else
			msg.setLocation(a);
		
		// Return the message
		return msg;
	}
}