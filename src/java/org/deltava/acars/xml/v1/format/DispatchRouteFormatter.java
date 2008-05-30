// Copyright 2007, 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import java.util.*;

import org.jdom.Element;

import org.deltava.beans.acars.RoutePlan;
import org.deltava.beans.navdata.*;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.dispatch.RouteInfoMessage;

import org.deltava.util.*;

/**
 * An XML formatter for dispatch route info messages.
 * @author Luke
 * @version 2.2
 * @since 2.0
 */

public class DispatchRouteFormatter extends ElementFormatter {

	/**
	 * Formats a RouteInfoMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	public Element format(Message msg) {

		// Cast the message
		RouteInfoMessage rmsg = (RouteInfoMessage) msg;
		
		// Build the DataResponseElement
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "routes");
		e.setAttribute("routeValid", String.valueOf(rmsg.isRouteValid()));
		
		// Add the routes
		for (Iterator<RoutePlan> i = rmsg.getPlans().iterator(); i.hasNext(); ) {
			RoutePlan rp = i.next();
			Element re = new Element("route");
			re.setAttribute("id", String.valueOf(rp.getID()));
			re.setAttribute("useCount", String.valueOf(rp.getUseCount()));
			re.addContent(XMLUtils.createElement("airline", rp.getAirline().getCode()));
			re.addContent(formatAirport(rp.getAirportD(), "airportD"));
			re.addContent(formatAirport(rp.getAirportA(), "airportA"));
			if (rp.getAirportL() != null)
				re.addContent(formatAirport(rp.getAirportL(), "airportL"));
			re.addContent(XMLUtils.createElement("sid", rp.getSID()));
			re.addContent(XMLUtils.createElement("star", rp.getSTAR()));
			re.addContent(XMLUtils.createElement("cruiseAlt", rp.getCruiseAltitude()));
			re.addContent(XMLUtils.createElement("comments", rp.getComments(), true));
			re.addContent(XMLUtils.createElement("route", rp.getRoute(), true));
			
			// Add the waypoints
			Element wpe = new Element("waypoints");
			for (Iterator<NavigationDataBean> ni = rp.getWaypoints().iterator(); ni.hasNext(); ) {
				NavigationDataBean nd = ni.next();
				Element we = new Element("waypoint");
				we.setAttribute("code", nd.getCode());
				we.setAttribute("lat", StringUtils.format(nd.getLatitude(), "##0.00000"));
				we.setAttribute("lon", StringUtils.format(nd.getLongitude(), "##0.00000"));
				we.setAttribute("uniqueID", nd.toString());
				String airway = rp.getAirway(nd);
				if (airway != null)
					we.setAttribute("airway", airway);
				
				wpe.addContent(we);
			}
			
			// Add the elements
			re.addContent(wpe);
			e.addContent(re);
		}
		
		return pe;
	}
}