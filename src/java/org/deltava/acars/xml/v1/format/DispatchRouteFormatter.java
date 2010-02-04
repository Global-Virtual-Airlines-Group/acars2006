// Copyright 2007, 2008, 2009, 2010 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import java.util.*;

import org.jdom.Element;

import org.deltava.beans.acars.DispatchRoute;
import org.deltava.beans.schedule.*;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.dispatch.RouteInfoMessage;

import org.deltava.util.*;

/**
 * An XML formatter for dispatch route info messages.
 * @author Luke
 * @version 3.0
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
		
		// Add the routes
		for (Iterator<? extends PopulatedRoute> i = rmsg.getPlans().iterator(); i.hasNext(); ) {
			PopulatedRoute rp = i.next();
			Element re = formatRoute(rp); 
			
			// Add external properties
			if (rp instanceof ExternalFlightRoute) {
				re.setAttribute("external", "true");
				re.addContent(XMLUtils.createElement("source", ((ExternalFlightRoute) rp).getSource(), true));
			}
			
			// Add dispatch route properties
			if (rp instanceof DispatchRoute) {
				DispatchRoute dr = (DispatchRoute) rp;
				re.setAttribute("useCount", String.valueOf(dr.getUseCount()));
				re.addContent(XMLUtils.createElement("airline", dr.getAirline().getCode()));
				if (dr.getAirportL() != null)
					re.addContent(formatAirport(dr.getAirportL(), "airportL"));
			}
			
			e.addContent(re);
		}
		
		return pe;
	}
}