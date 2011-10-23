// Copyright 2006, 2007, 2009, 2010, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import java.util.*;

import org.jdom.Element;

import org.deltava.acars.xml.*;

import org.deltava.beans.navdata.NavigationDataBean;
import org.deltava.beans.schedule.*;

import org.deltava.util.*;
import org.deltava.util.system.SystemData;

/**
 * A formatter to create XML command elements.
 * @author Luke
 * @version 4.1
 * @since 1.0
 */

abstract class ElementFormatter extends XMLElementFormatter {

	/**
	 * Helper method to format an Airport bean.
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

			// Add UTC offset
			TimeZone tz = a.getTZ().getTimeZone();
			long ofs = tz.getOffset(System.currentTimeMillis()) / 1000;
			ae.setAttribute("utcOffset", String.valueOf(ofs));

			// Attach airlines
			for (Iterator<String> i = a.getAirlineCodes().iterator(); i.hasNext();) {
				String aCode = i.next();
				Airline al = SystemData.getAirline(aCode);
				if (al == null)
					continue;

				// Build the airline element
				Element ale = new Element("airline");
				ale.setAttribute("code", al.getCode());
				ale.setAttribute("name", al.getName());
				ae.addContent(ale);
			}
		}

		return ae;
	}

	/**
	 * Helper method to format a flight route.
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
			for (Iterator<NavigationDataBean> ni = pr.getWaypoints().iterator(); ni.hasNext();) {
				NavigationDataBean nd = ni.next();
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