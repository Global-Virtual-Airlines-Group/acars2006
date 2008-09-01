// Copyright 2006, 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import java.util.*;

import org.jdom.Element;

import org.deltava.beans.navdata.NavigationDataBean;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.dispatch.FlightDataMessage;

import org.deltava.util.*;

/**
 * An XML Formatter for DispatchInfo messages.
 * @author Luke
 * @version 2.1
 * @since 1.0
 */

class DispatchInfoFormatter extends ElementFormatter {

	/**
	 * Formats a DispatchMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	public Element format(Message msg) {

		// Cast the message
		FlightDataMessage dmsg = (FlightDataMessage) msg;

		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, dmsg.getRequestTypeName());
		e.setAttribute("routeValid", String.valueOf(dmsg.isRouteValid()));
		e.setAttribute("id", String.valueOf(dmsg.getRouteID()));
		e.addContent(XMLUtils.createElement("airline", dmsg.getAirline().getCode()));
		e.addContent(XMLUtils.createElement("flight", String.valueOf(dmsg.getFlight())));
		e.addContent(XMLUtils.createElement("leg", String.valueOf(dmsg.getLeg())));
		e.addContent(XMLUtils.createElement("eqType", dmsg.getEquipmentType()));
		e.addContent(XMLUtils.createElement("cruiseAlt", dmsg.getCruiseAltitude()));
		e.addContent(XMLUtils.createElement("tx", StringUtils.format(dmsg.getTXCode(), "0000")));
		e.addContent(formatAirport(dmsg.getAirportD(), "airportD"));
		e.addContent(formatAirport(dmsg.getAirportA(), "airportA"));
		if (dmsg.getAirportL() != null)
			e.addContent(formatAirport(dmsg.getAirportL(), "airportL"));
		if (dmsg.getSID() != null)
			e.addContent(XMLUtils.createElement("sid", dmsg.getSID().toString()));
		if (dmsg.getSTAR() != null)
			e.addContent(XMLUtils.createElement("star", dmsg.getSTAR().toString()));

		// Add fuel tanks
		Element fe = new Element("fuel");
		e.addContent(fe);
		Map<FuelTank, Integer> fuelInfo = dmsg.getFuel();
		for (Iterator<FuelTank> i = fuelInfo.keySet().iterator(); i.hasNext(); ) {
			FuelTank t = i.next();
			Element fte = new Element("tank");
			fte.setAttribute("name", t.getName());
			fte.setAttribute("load", fuelInfo.get(t).toString());
			fe.addContent(fte);
		}
		
		// Add waypoints
		Element re = new Element("route");
		e.addContent(re);
		for (Iterator<RouteWaypoint> i = dmsg.getWaypoints().iterator(); i.hasNext(); ) {
			RouteWaypoint wp = i.next();
			NavigationDataBean nd = wp.getWaypoint();
			Element wpe = new Element("waypoint");
			wpe.setAttribute("code", nd.getCode());
			wpe.setAttribute("type", nd.getTypeName());
			wpe.setAttribute("lat", StringUtils.format(nd.getLatitude(), "#0.00000"));
			wpe.setAttribute("lon", StringUtils.format(nd.getLongitude(), "#0.00000"));
			wpe.setAttribute("uniqueID", nd.toString());
			if (nd.getRegion() != null)
				wpe.setAttribute("region", nd.getRegion());
			if (wp.getAirway() != null)
				wpe.setAttribute("airway", wp.getAirway());
			if (wp.isInTerminalRoute())
				wpe.setAttribute("tr", String.valueOf(wp.isInTerminalRoute()));
			
			re.addContent(wpe);
		}

		return pe;
	}
}