// Copyright 2009, 2011, 2012, 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import org.jdom2.Element;

import org.deltava.beans.acars.TaxiTime;
import org.deltava.beans.navdata.Runway;
import org.deltava.beans.wx.METAR;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.AirportInfoMessage;

import org.deltava.util.*;

/**
 * A formatter for AirportInfoMessages.
 * @author Luke
 * @version 10.0
 * @since 2.6
 */

public class AirportInfoFormatter extends ElementFormatter {

	/**
	 * Formats an AirportInfoMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {
		
		// Cast the message
		AirportInfoMessage amsg = (AirportInfoMessage) msg;

		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "airportinfo");
		
		// Save the airport data
		Element ae = new Element("info");
		ae.setAttribute("icao", amsg.getAirport().getICAO());
		ae.addContent(formatAirport(amsg.getAirport(), "airport"));
		if (amsg.getMETAR() != null) {
			METAR wx = amsg.getMETAR();
			Element ew = XMLUtils.createElement("wx", wx.getData(), true);
			ew.setAttribute("type", wx.getType().toString());
			ew.setAttribute("valid", StringUtils.format(wx.getDate(), "MM/dd/yyyy HH:mm"));
			ew.setAttribute("temp", String.valueOf(wx.getTemperature()));
			ew.setAttribute("wSpeed", String.valueOf(wx.getWindSpeed()));
			ew.setAttribute("wDir", String.valueOf(wx.getWindDirection()));
			ew.setAttribute("wGust", String.valueOf(wx.getWindGust()));
			ae.addContent(ew);
		}
		
		// Add the runways
		for (Runway rwy : amsg.getResponse()) {
			Element re = new Element("runway");
			re.setAttribute("name", rwy.getName());
			re.setAttribute("length", String.valueOf(rwy.getLength()));
			re.setAttribute("hdg", String.valueOf(rwy.getHeading()));
			ae.addContent(re);
		}
		
		// Add taxi times
		if (amsg.getTaxiTime() != null) {
			TaxiTime tt = amsg.getTaxiTime();
			Element tte = new Element("taxiTime");
			tte.setAttribute("in", String.valueOf(tt.getInboundTime().toSeconds()));
			tte.setAttribute("out", String.valueOf(tt.getOutboundTime().toSeconds()));
			ae.addContent(tte);
		}
		
		e.addContent(ae);
		return pe;
	}
}