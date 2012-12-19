// Copyright 2007, 2008, 2009, 2010, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import org.jdom2.Element;

import org.deltava.beans.Flight;
import org.deltava.beans.acars.DispatchRoute;
import org.deltava.beans.navdata.Gate;
import org.deltava.beans.schedule.*;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.dispatch.RouteInfoMessage;

import org.deltava.util.*;
import org.deltava.util.system.SystemData;

/**
 * An XML formatter for dispatch route info messages.
 * @author Luke
 * @version 5.1
 * @since 2.0
 */

public class DispatchRouteFormatter extends ElementFormatter {

	/**
	 * Formats a RouteInfoMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {

		// Cast the message
		RouteInfoMessage rmsg = (RouteInfoMessage) msg;
		
		// Build the DataResponseElement
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "routes");
		e.setAttribute("routeValid", String.valueOf(rmsg.isRouteValid()));
		e.setAttribute("id", String.valueOf(rmsg.getParentID()));
		if (!StringUtils.isEmpty(rmsg.getMessage()))
			e.addContent(XMLUtils.createElement("msg", rmsg.getMessage(), true));
		
		// Get flight data
		Flight f = rmsg.getScheduleInfo();
		if (f != null) {
			e.setAttribute("airline", f.getAirline().getCode());
			e.setAttribute("flight", String.valueOf(f.getFlightNumber()));
			e.setAttribute("leg", String.valueOf(f.getLeg()));
		}
		
		// Add the gates
		if (rmsg.getClass() != null)
			e.addContent(formatGate(rmsg.getClosestGate(), "gateD"));
		for (Gate g : rmsg.getArrivalGates())
			e.addContent(formatGate(g, "gateA"));
		
		// Add the routes
		for (PopulatedRoute rp : rmsg.getPlans()) {
			Element re = formatRoute(rp);
			if (rp instanceof ExternalFlightRoute) {
				re.setAttribute("external", "true");
				re.addContent(XMLUtils.createElement("source", ((ExternalFlightRoute) rp).getSource(), true));
			}
			
			// Add dispatch route properties
			if (rp instanceof DispatchRoute) {
				DispatchRoute dr = (DispatchRoute) rp;
				re.setAttribute("useCount", String.valueOf(dr.getUseCount()));
				Airline a = dr.getAirline();
				if ((a == null) && (f != null))
					a = f.getAirline();
				if (a == null)
					a = SystemData.getAirline(msg.getSender().getAirlineCode());
				if (a != null)
					re.addContent(XMLUtils.createElement("airline", a.getCode()));
				if (dr.getAirportL() != null)
					re.addContent(formatAirport(dr.getAirportL(), "airportL"));
			} else if (f != null)
				re.addContent(XMLUtils.createElement("airline", f.getAirline().getCode())); // Hack for Build 97
			
			e.addContent(re);
		}
		
		return pe;
	}
}