// Copyright 2006, 2007, 2009, 2010, 2011, 2012, 2013, 2016, 2018, 2020, 2021, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

import java.time.*;

import org.jdom2.Element;

import org.deltava.acars.xml.*;

import org.deltava.beans.navdata.NavigationDataBean;
import org.deltava.beans.schedule.*;

import org.deltava.util.*;
import org.deltava.util.system.SystemData;

/**
 * A formatter to create XML command elements.
 * @author Luke
 * @version 11.0
 * @since 1.0
 */

abstract class ElementFormatter extends XMLElementFormatter {

	/**
	 * Helper method to format an Airport bean.
	 * @param a the Airport bean
	 * @param eName the element name
	 * @return an Element
	 */
	protected static Element formatAirport(Airport a, String eName) {
		Element ae = new Element(eName);
		if (a == null) return ae;
		
		ae.setAttribute("name", a.getName());
		ae.setAttribute("icao", a.getICAO());
		ae.setAttribute("iata", a.getIATA());
		ae.setAttribute("country", a.getCountry().getCode());
		ae.setAttribute("lat", StringUtils.format(a.getLatitude(), "##0.0000"));
		ae.setAttribute("lng", StringUtils.format(a.getLongitude(), "##0.0000"));
		ae.setAttribute("adse", String.valueOf(a.getASDE()));
		ae.setAttribute("hasUSPFI", String.valueOf(a.getHasPFI()));
		ae.setAttribute("isSchengen", String.valueOf(a.getIsSchengen()));
		ae.setAttribute("maxRunwayLength", String.valueOf(a.getMaximumRunwayLength()));
		if (a.getSupercededAirport() != null)
			ae.setAttribute("supercedes", a.getSupercededAirport());

		// Add UTC offset
		ZoneId tz = a.getTZ().getZone();
		int ofs = tz.getRules().getOffset(Instant.now()).getTotalSeconds();
		ae.setAttribute("utcOffset", String.valueOf(ofs));

		// Attach airlines
		a.getAirlineCodes().stream().map(SystemData::getAirline).filter(java.util.Objects::nonNull).forEach(al -> {
			Element ale = new Element("airline");
			ale.setAttribute("code", al.getCode());
			ale.setAttribute("name", al.getName());
			ae.addContent(ale);	
		});

		return ae;
	}

	/**
	 * Helper method to format a flight route.
	 * @param rt the flightRoute
	 * @return an Element
	 */
	protected static Element formatRoute(FlightRoute rt) {
		if (rt == null) return null;
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
		if (rt instanceof PopulatedRoute pr) {
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
}