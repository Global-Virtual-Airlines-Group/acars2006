// Copyright 2018, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

import org.jdom2.Element;

import org.deltava.beans.UseCount;
import org.deltava.beans.navdata.Runway;

import org.deltava.util.*;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.RunwayListMessage;

/**
 * A formatter for Runway List messages.
 * @author Luke
 * @version 8.6
 * @since 8.4
 */

class RunwayListFormatter extends ElementFormatter {

	/**
	 * Formats a RunwayListMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {
		
		// Cast the message
		RunwayListMessage rmsg = (RunwayListMessage) msg;

		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "runways");
		e.setAttribute("count", String.valueOf(rmsg.getResponse().size()));
		e.setAttribute("isPopular", String.valueOf(rmsg.isPopular()));
		e.setAttribute("airportD", rmsg.getAirportD().getICAO());
		if (rmsg.getAirportA() != null)
			e.setAttribute("airportA", rmsg.getAirportA().getICAO());
		
		for (Runway r : rmsg.getResponse()) {
			Element re = new Element("runway");
			re.setAttribute("lat", StringUtils.format(r.getLatitude(), "##0.0000"));
			re.setAttribute("lng", StringUtils.format(r.getLongitude(), "##0.0000"));
			re.setAttribute("icao", r.getCode());
			re.setAttribute("name", r.getName());
			re.setAttribute("hdg", String.valueOf(r.getHeading()));
			re.setAttribute("length", String.valueOf(r.getLength()));
			re.setAttribute("surface", r.getSurface().getName());
			if (r.getNewCode() != null)
				re.setAttribute("oldCode", r.getNewCode());
			if ((r.getFrequency() != null) && (!"-".equals(r.getFrequency())))
				re.addContent(XMLUtils.createElement("freq", r.getFrequency()));
			if (r instanceof UseCount)
				re.setAttribute("useCount", String.valueOf(((UseCount) r).getUseCount()));
			
			e.addContent(re);
		}

		return pe;
	}
}