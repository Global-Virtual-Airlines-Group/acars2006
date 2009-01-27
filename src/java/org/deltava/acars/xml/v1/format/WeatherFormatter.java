// Copyright 2008, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import java.util.Iterator;

import org.jdom.Element;

import org.deltava.beans.wx.WeatherDataBean;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.WXMessage;

import org.deltava.util.*;

/**
 * An XML formatter for Weather data messages.
 * @author Luke
 * @version 2.4
 * @since 2.2
 */

class WeatherFormatter extends ElementFormatter {

	/**
	 * Formats a WXMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	public Element format(Message msg) {
		
		// Cast the message
		WXMessage wxmsg = (WXMessage) msg;
		if (wxmsg.getAirport() == null)
			return null;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "weather");
		e.setAttribute("icao", wxmsg.getAirport().getICAO());
		for (Iterator<WeatherDataBean> i = wxmsg.getResponse().iterator(); i.hasNext(); ) {
			WeatherDataBean wx = i.next();
			Element ew = XMLUtils.createElement("wx", wx.getData(), true);
			ew.setAttribute("type", wx.getType());
			ew.setAttribute("valid", StringUtils.format(wx.getDate(), "MM/dd/yyyy HH:mm"));
			e.addContent(ew);
		}
		
		return pe;
	}
}