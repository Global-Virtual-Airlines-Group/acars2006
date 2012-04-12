// Copyright 2006, 2007, 2008, 2010, 2011, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import java.util.*;

import org.jdom2.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.AircraftMessage;

import org.deltava.beans.schedule.Aircraft;
import org.deltava.beans.system.AirlineInformation;

import org.deltava.util.*;

/**
 * An XML Formatter for Aircraft data messages.
 * @author Luke
 * @version 4.2
 * @since 1.0
 */

class AircraftFormatter extends ElementFormatter {

	/**
	 * Formats an AircraftMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {

		// Cast the message
		AircraftMessage amsg = (AircraftMessage) msg;

		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "info");
		for (Iterator<Aircraft> i = amsg.getResponse().iterator(); i.hasNext(); ) {
			Aircraft a = i.next();
			
			// Build the aircraft element
			Element ae = new Element("eqtype");
			if (amsg.getShowProfile()) {
				ae.setAttribute("name", a.getName());
				ae.setAttribute("engines", String.valueOf(a.getEngines()));
				ae.setAttribute("seats", String.valueOf(a.getSeats()));
				ae.setAttribute("historic", String.valueOf(a.getHistoric()));
				ae.setAttribute("etops", String.valueOf(a.getETOPS()));
				ae.addContent(XMLUtils.createElement("fullName", a.getFullName()));
				ae.addContent(XMLUtils.createElement("family", a.getFamily()));
				ae.addContent(XMLUtils.createElement("engineType", a.getEngineType()));
				ae.addContent(XMLUtils.createElement("range", String.valueOf(a.getRange())));
				ae.addContent(XMLUtils.createElement("cruiseSpeed", String.valueOf(a.getCruiseSpeed())));
				ae.addContent(XMLUtils.createElement("baseFuel", String.valueOf(a.getBaseFuel())));
				ae.addContent(XMLUtils.createElement("taxiFuel", String.valueOf(a.getTaxiFuel())));
				ae.addContent(XMLUtils.createElement("fuelFlow", String.valueOf(a.getFuelFlow())));
				ae.addContent(XMLUtils.createElement("maxWeight", String.valueOf(a.getMaxWeight())));
				ae.addContent(XMLUtils.createElement("maxTakeoffWeight", String.valueOf(a.getMaxTakeoffWeight())));
				ae.addContent(XMLUtils.createElement("maxLandingWeight", String.valueOf(a.getMaxLandingWeight())));
				ae.addContent(XMLUtils.createElement("toRunwayLength", String.valueOf(a.getTakeoffRunwayLength())));
				ae.addContent(XMLUtils.createElement("lndRunwayLength", String.valueOf(a.getLandingRunwayLength())));
				
				// Dump IATA codes
				for (String iataCode : a.getIATA())
					ae.addContent(XMLUtils.createElement("iata", iataCode, false));
				
				// Dump app names
				for (AirlineInformation ai : a.getApps())
					ae.addContent(XMLUtils.createElement("app", ai.getCode()));
				
				// Get tank names/percentages
				Map<String, Collection<String>> tNames = a.getTankNames();
				Map<String, Integer> tPct = a.getTankPercent();
				ae.addContent(XMLUtils.createElement("pTanks", StringUtils.listConcat(tNames.get("Primary"), ",")));
				ae.addContent(XMLUtils.createElement("pPct", String.valueOf(tPct.get("Primary"))));
				ae.addContent(XMLUtils.createElement("sTanks", StringUtils.listConcat(tNames.get("Secondary"), ",")));
				ae.addContent(XMLUtils.createElement("sPct", String.valueOf(tPct.get("Secondary"))));
				ae.addContent(XMLUtils.createElement("oTanks", StringUtils.listConcat(tNames.get("Other"), ",")));
			} else
				ae.setText(a.getName());
			
			e.addContent(ae);
		}
		
		return pe;
	}
}