// Copyright 2006, 2007, 2008, 2011, 2012, 2018, 2020, 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import java.util.*;

import org.jdom2.Element;

import org.deltava.beans.navdata.*;
import org.deltava.beans.schedule.FuelTank;

import org.deltava.acars.message.*;
import org.deltava.acars.message.dispatch.FlightDataMessage;

import org.deltava.util.*;

/**
 * An XML Formatter for DispatchInfo messages.
 * @author Luke
 * @version 10.0
 * @since 1.0
 */

class DispatchInfoFormatter extends ElementFormatter {

	/**
	 * Formats a DispatchMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {

		// Cast the message
		FlightDataMessage dmsg = (FlightDataMessage) msg;

		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, dmsg.getRequestType().getCode());
		e.setAttribute("routeValid", String.valueOf(dmsg.isRouteValid()));
		e.setAttribute("id", String.valueOf(dmsg.getRouteID()));
		e.setAttribute("logID", String.valueOf(dmsg.getLogID()));
		e.addContent(XMLUtils.createElement("airline", dmsg.getAirline().getCode()));
		e.addContent(XMLUtils.createElement("flight", String.valueOf(dmsg.getFlight())));
		e.addContent(XMLUtils.createElement("leg", String.valueOf(dmsg.getLeg())));
		e.addContent(XMLUtils.createElement("eqType", dmsg.getEquipmentType()));
		e.addContent(XMLUtils.createElement("cruiseAlt", dmsg.getCruiseAltitude()));
		e.addContent(XMLUtils.createElement("tx", StringUtils.format(dmsg.getTXCode(), "0000")));
		e.addContent(formatAirport(dmsg.getAirportD(), "airportD"));
		e.addContent(formatAirport(dmsg.getAirportA(), "airportA"));
		XMLUtils.addIfPresent(e, formatAirport(dmsg.getAirportL(), "airportL"));
		XMLUtils.addIfPresent(e, XMLUtils.createIfPresent("sid", dmsg.getSID()));
		XMLUtils.addIfPresent(e, XMLUtils.createIfPresent("star", dmsg.getSTAR()));
		XMLUtils.addIfPresent(e, formatGate(dmsg.getGateD(), "gateD"));
		XMLUtils.addIfPresent(e, formatGate(dmsg.getGateA(), "gateA"));

		// Add fuel tanks
		Element fe = new Element("fuel");
		e.addContent(fe);
		for (Map.Entry<FuelTank, Integer> me : dmsg.getFuel().entrySet()) {
			Element fte = new Element("tank");
			fte.setAttribute("name", me.getKey().getName());
			fte.setAttribute("load", me.getValue().toString());
			fe.addContent(fte);
		}
		
		// Add waypoints
		Element re = XMLUtils.createElement("route", dmsg.getRoute(), true);
		e.addContent(re);
		for (NavigationDataBean nd : dmsg.getWaypoints()) {
			Element wpe = new Element("waypoint");
			wpe.setAttribute("code", nd.getCode());
			wpe.setAttribute("type", nd.getType().getName());
			wpe.setAttribute("lat", StringUtils.format(nd.getLatitude(), "#0.00000"));
			wpe.setAttribute("lon", StringUtils.format(nd.getLongitude(), "#0.00000"));
			wpe.setAttribute("uniqueID", nd.toString());
			if (nd.getRegion() != null)
				wpe.setAttribute("region", nd.getRegion());
			if ((nd.getAirway() != null) && (nd.getAirway().length() > 1))
				wpe.setAttribute("airway", nd.getAirway());
			if (nd.isInTerminalRoute())
				wpe.setAttribute("tr", String.valueOf(nd.isInTerminalRoute()));
			
			re.addContent(wpe);
		}

		return pe;
	}
}