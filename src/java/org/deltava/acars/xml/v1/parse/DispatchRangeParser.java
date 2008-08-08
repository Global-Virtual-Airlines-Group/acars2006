// Copyright 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import org.jdom.Element;

import org.deltava.beans.Pilot;
import org.deltava.beans.schedule.GeoPosition;

import org.deltava.acars.message.dispatch.RangeMessage;

import org.deltava.util.StringUtils;

/**
 * A parser for Dispatch service range elements.
 * @author Luke
 * @version 2.2
 * @since 2.2
 */

public class DispatchRangeParser extends ElementParser<RangeMessage> {

	/**
	 * Convert an XML dispatcher range element into a RangeMessage.
	 * @param e the XML element
	 * @param user the originating user
	 * @return a RangeMessage
	 */
	public RangeMessage parse(Element e, Pilot user) {
		
		// Create the message
		RangeMessage msg = new RangeMessage(user);
		
		// Get the range Data
		msg.setRange(StringUtils.parse(getChildText(e, "range", null), Integer.MAX_VALUE));
		double lat = StringUtils.parse(getChildText(e, "lat", "0.0"), 0.0d);
		double lng = StringUtils.parse(getChildText(e, "lng", "0.0"), 0.0d);
		msg.setLocation(new GeoPosition(lat, lng));
		
		// Return the message
		return msg;
	}
}