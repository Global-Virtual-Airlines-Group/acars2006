// Copyright 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import java.text.*;
import java.util.*;

import org.jdom.Element;

import org.deltava.beans.MapEntry;
import org.deltava.beans.navdata.NavigationDataBean;
import org.deltava.beans.schedule.*;
import org.deltava.util.*;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.OceanicTrackMessage;

/**
 * An XML Formatter for Oceanic Route messages.
 * @author Luke
 * @version 2.2
 * @since 2.2
 */

public class OceanicRouteFormatter extends ElementFormatter {

	/**
	 * Formats an OceanicTrackMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	public Element format(Message msg) {
		
		// Cast the message
		OceanicTrackMessage omsg = (OceanicTrackMessage) msg;
		Date dt = omsg.getResponse().get(0).getDate();
		final NumberFormat nf = new DecimalFormat("##0.0000");
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "nat");
		e.setAttribute("date", StringUtils.format(dt, "MM/dd/yyyy"));
		for (Iterator<OceanicWaypoints> i = omsg.getResponse().iterator(); i.hasNext(); ) {
			OceanicWaypoints ow = i.next();
			boolean isEast = (ow.getDirection() == OceanicWaypoints.EAST);
			
			// Build the track element
			Element te = new Element("track");
			te.setAttribute("code", ow.getTrack());
			te.setAttribute("type", ow.isFixed() ? "C" : (isEast ? "E" : "W"));
			te.setAttribute("color", ow.isFixed() ? "#2040E0" : (isEast ? "#EEEEEE" : "#EEEE44"));
			te.setAttribute("track", ow.getWaypointCodes());
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