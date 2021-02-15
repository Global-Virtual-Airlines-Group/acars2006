// Copyright 2006, 2012, 2016, 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import org.jdom2.Element;

import org.deltava.acars.beans.NavigationRadioBean;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.NavigationDataMessage;

import org.deltava.beans.navdata.*;
import org.deltava.util.*;

/**
 * An XML Formatter for Navigation Data messages.
 * @author Luke
 * @version 10.0
 * @since 1.0
 */

class NavinfoFormatter extends ElementFormatter {

	/**
	 * Formats a NavigationDataMessage into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {
		
		// Get the bean - navdata messages only have one response entry
		NavigationDataMessage ndmsg = (NavigationDataMessage) msg;
		if (ndmsg.getResponse().isEmpty()) return null;
		NavigationDataBean navaid = ndmsg.getResponse().get(0);
		
		// Create the element
		Element pe = initResponse(msg.getType());
		if (navaid instanceof Runway) {
			Runway r = (Runway) navaid;
			Element e = initDataResponse(pe, "runways");
			Element re = new Element("runway");
			re.setAttribute("lat", StringUtils.format(r.getLatitude(), "##0.0000"));
			re.setAttribute("lng", StringUtils.format(r.getLongitude(), "##0.0000"));
			re.setAttribute("icao", r.getCode());
			re.setAttribute("name", r.getName());
			re.setAttribute("hdg", String.valueOf(r.getHeading()));
			re.setAttribute("length", String.valueOf(r.getLength()));
			re.setAttribute("surface", r.getSurface().getName());
			if ((r.getFrequency() != null) && (!"-".equals(r.getFrequency())))
				re.addContent(XMLUtils.createElement("freq", r.getFrequency()));

			e.addContent(re);
		} else if (navaid instanceof NavigationRadioBean) {
			NavigationRadioBean nrb = (NavigationRadioBean) navaid;
			Element e = initDataResponse(pe, "navaid");
			Element ne = new Element("navaid");
			ne.setAttribute("lat", StringUtils.format(nrb.getLatitude(), "##0.0000"));
			ne.setAttribute("lng", StringUtils.format(nrb.getLongitude(), "##0.0000"));
			if (nrb.getRadio() != null)
				ne.addContent(XMLUtils.createElement("radio", nrb.getRadio()));
			ne.addContent(XMLUtils.createElement("type", navaid.getType().getName()));
			ne.addContent(XMLUtils.createElement("code", navaid.getCode()));
			ne.addContent(XMLUtils.createElement("name", navaid.getName()));
			if (navaid.getType() == Navaid.VOR) {
				ne.addContent(XMLUtils.createElement("freq", nrb.getFrequency()));
				if (!StringUtils.isEmpty(nrb.getHeading()))
					ne.addContent(XMLUtils.createElement("hdg", nrb.getHeading()));
			} else if (navaid.getType() == Navaid.NDB)
				ne.addContent(XMLUtils.createElement("freq", nrb.getFrequency()));

			e.addContent(ne);
		}
		
		return pe;
	}
}