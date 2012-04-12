// Copyright 2008, 2009, 2011, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import org.jdom2.Element;

import org.deltava.beans.flight.ILSCategory;
import org.deltava.beans.wx.*;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.WXMessage;

import org.deltava.util.*;

/**
 * An XML formatter for Weather data messages.
 * @author Luke
 * @version 4.2
 * @since 2.2
 */

class WeatherFormatter extends ElementFormatter {

	/**
	 * Formats a WXMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {
		
		// Cast the message
		WXMessage wxmsg = (WXMessage) msg;
		if (wxmsg.getAirport() == null)
			return null;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "weather");
		e.setAttribute("icao", wxmsg.getAirport().getICAO());
		for (WeatherDataBean wx : wxmsg.getResponse()) {
			if (wx.getDate() != null) {
				Element ew = XMLUtils.createElement("wx", wx.getData(), true);
				ew.setAttribute("type", wx.getType().toString());
				ew.setAttribute("valid", StringUtils.format(wx.getDate(), "MM/dd/yyyy HH:mm"));
				if (wx instanceof METAR) {
					METAR m = (METAR) wx;
					ew.setAttribute("temp", String.valueOf(m.getTemperature()));
					ew.setAttribute("wSpeed", String.valueOf(m.getWindSpeed()));
					ew.setAttribute("wDir", String.valueOf(m.getWindDirection()));
					ew.setAttribute("wGust", String.valueOf(m.getWindGust()));
					if (m.getILS() != ILSCategory.NONE)
						ew.setAttribute("ils", m.getILS().toString());
				}
				
				e.addContent(ew);
			}
		}
		
		return pe;
	}
}