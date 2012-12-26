// Copyright 2006, 2007, 2009, 2010, 2011, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import java.util.*;

import org.jdom2.Element;

import org.deltava.acars.xml.*;

import org.deltava.beans.navdata.*;
import org.deltava.beans.schedule.*;

import org.deltava.util.*;
import org.deltava.util.system.SystemData;

/**
 * A formatter to create XML data elements.
 * @author Luke
 * @version 5.1
 * @since 1.0
 */

abstract class ElementFormatter extends XMLElementFormatter {

	/**
	 * Helper method to format an Airport bean.
	 * @param a the Airport
	 * @param eName the XML element name
	 */
	protected static Element formatAirport(Airport a, String eName) {
		Element ae = new Element(eName);
		if (a != null) {
			ae.setAttribute("name", a.getName());
			ae.setAttribute("icao", a.getICAO());
			ae.setAttribute("iata", a.getIATA());
			ae.setAttribute("country", a.getCountry().getCode());
			ae.setAttribute("lat", StringUtils.format(a.getLatitude(), "##0.0000"));
			ae.setAttribute("lng", StringUtils.format(a.getLongitude(), "##0.0000"));
			ae.setAttribute("adse", String.valueOf(a.getADSE()));
			ae.setAttribute("maxRunwayLength", String.valueOf(a.getMaximumRunwayLength()));

			// Add UTC offset
			TimeZone tz = a.getTZ().getTimeZone();
			long ofs = tz.getOffset(System.currentTimeMillis()) / 1000;
			ae.setAttribute("utcOffset", String.valueOf(ofs));

			// Attach airlines
			for (String aCode : a.getAirlineCodes()) {
				final Airline al = SystemData.getAirline(aCode);
				if (al == null)
					continue;

				Element ale = new Element("airline") {{ setAttribute("code", al.getCode()); setAttribute("name", al.getName()); }};
				ae.addContent(ale);
			}
		}

		return ae;
	}

	/**
	 * Helper method to format a flight route.
	 * @param rt the FlightRoute
	 */
	protected static Element formatRoute(FlightRoute rt) {
		Element re = new Element("route");
		re.setAttribute("id", String.valueOf(rt.getID()));
		re.addContent(formatAirport(rt.getAirportD(), "airportD"));
		re.addContent(formatAirport(rt.getAirportA(), "airportA"));
		re.addContent(XMLUtils.createElement("sid", rt.getSID()));
		re.addContent(XMLUtils.createElement("star", rt.getSTAR()));
		re.addContent(XMLUtils.createElement("cruiseAlt", rt.getCruiseAltitude()));
		re.addContent(XMLUtils.createElement("comments", rt.getComments(), true));
		re.addContent(XMLUtils.createElement("route", rt.getRoute(), true));

		// Add the waypoints
		if (rt instanceof PopulatedRoute) {
			PopulatedRoute pr = (PopulatedRoute) rt;
			Element wpe = new Element("waypoints");
			for (NavigationDataBean nd : pr.getWaypoints()) {
				Element we = new Element("waypoint");
				we.setAttribute("code", nd.getCode());
				we.setAttribute("lat", StringUtils.format(nd.getLatitude(), "##0.00000"));
				we.setAttribute("lon", StringUtils.format(nd.getLongitude(), "##0.00000"));
				we.setAttribute("uniqueID", nd.toString());
				if (nd.getRegion() != null) we.setAttribute("region", nd.getRegion());
				String airway = pr.getAirway(nd);
				if (airway != null) we.setAttribute("airway", airway);
				wpe.addContent(we);
			}

			re.addContent(wpe);
		}

		return re;
	}

	/**
	 * Helper method to format a Gate.
	 * @param g the Gate
	 * @param eName the XML element name
	 */
	protected static Element formatGate(Gate g, String eName) {
		Element ge = new Element(eName);		
		ge.setAttribute("icao", g.getCode());
		ge.setAttribute("code", g.getName());
		ge.setAttribute("lat", StringUtils.format(g.getLatitude(), "#0.00000"));
		ge.setAttribute("lon", StringUtils.format(g.getLongitude(), "#0.00000"));
		return ge;
	}
}