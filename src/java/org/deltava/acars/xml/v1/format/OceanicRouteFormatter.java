// Copyright 2008, 2009, 2010, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import java.text.*;
import java.util.*;

import org.jdom2.Element;

import org.deltava.beans.MapEntry;
import org.deltava.beans.navdata.*;
import org.deltava.util.*;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.OceanicTrackMessage;

/**
 * An XML Formatter for Oceanic Route messages.
 * @author Luke
 * @version 4.2
 * @since 2.2
 */

public class OceanicRouteFormatter extends ElementFormatter {

	/**
	 * Formats an OceanicTrackMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {
		
		// Cast the message
		OceanicTrackMessage omsg = (OceanicTrackMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "nat");
		if (omsg.getResponse().isEmpty())
			return pe;
		
		// Get the date
		e.setAttribute("date", StringUtils.format(omsg.getDate(), "MM/dd/yyyy"));
		final NumberFormat nf = new DecimalFormat("##0.0000");
		for (Iterator<OceanicTrack> i = omsg.getResponse().iterator(); i.hasNext(); ) {
			OceanicTrack ow = i.next();
			boolean isEast = (ow.getDirection() == OceanicTrackInfo.Direction.EAST);
			
			// Build the track element
			Element te = new Element("track");
			te.setAttribute("code", ow.getTrack());
			te.setAttribute("route", ow.getTypeName());
			te.setAttribute("type", ow.isFixed() ? "C" : (isEast ? "E" : "W"));
			te.setAttribute("color", ow.isFixed() ? "#2040E0" : (isEast ? "#EEEEEE" : "#EEEE44"));
			te.setAttribute("track", ow.getRoute());
			for (Iterator<NavigationDataBean> wi = ow.getWaypoints().iterator(); wi.hasNext();) {
				NavigationDataBean ndb = wi.next();
				Element we = XMLUtils.createElement("waypoint", ndb.getInfoBox(), true);
				we.setAttribute("code", ndb.getCode());
				we.setAttribute("lat", nf.format(ndb.getLatitude()));
				we.setAttribute("lng", nf.format(ndb.getLongitude()));
				we.setAttribute("color", ow.isFixed() ? MapEntry.BLUE : (isEast ? MapEntry.WHITE : MapEntry.ORANGE));
				te.addContent(we);
			}
			
			// Add the element
			e.addContent(te);
		}
		
		return pe;
	}
}