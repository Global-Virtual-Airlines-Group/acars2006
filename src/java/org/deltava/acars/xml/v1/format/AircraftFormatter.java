// Copyright 2006, 2007, 2008, 2010, 2011, 2012, 2019, 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import java.util.*;

import org.jdom2.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.AircraftMessage;
import org.deltava.beans.flight.ETOPS;
import org.deltava.beans.schedule.*;

import org.deltava.util.*;

/**
 * An XML Formatter for Aircraft data messages.
 * @author Luke
 * @version 9.0
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
		for (Aircraft a : amsg.getResponse()) {
			Element ae = new Element("eqtype");
			if (!amsg.getShowProfile()) {
				ae.setText(a.getName());
				continue;
			}
			
			ae.setAttribute("name", a.getName());
			ae.setAttribute("engines", String.valueOf(a.getEngines()));
			ae.setAttribute("historic", String.valueOf(a.getHistoric()));
			ae.setAttribute("academyOnly", String.valueOf(a.getAcademyOnly()));
			ae.addContent(XMLUtils.createElement("fullName", a.getFullName()));
			ae.addContent(XMLUtils.createElement("family", a.getFamily()));
			ae.addContent(XMLUtils.createElement("engineType", a.getEngineType()));
			ae.addContent(XMLUtils.createElement("cruiseSpeed", String.valueOf(a.getCruiseSpeed())));
			ae.addContent(XMLUtils.createElement("baseFuel", String.valueOf(a.getBaseFuel())));
			ae.addContent(XMLUtils.createElement("taxiFuel", String.valueOf(a.getTaxiFuel())));
			ae.addContent(XMLUtils.createElement("fuelFlow", String.valueOf(a.getFuelFlow())));
			ae.addContent(XMLUtils.createElement("maxWeight", String.valueOf(a.getMaxWeight())));
			ae.addContent(XMLUtils.createElement("maxZFW", String.valueOf(a.getMaxZeroFuelWeight())));
			ae.addContent(XMLUtils.createElement("maxTakeoffWeight", String.valueOf(a.getMaxTakeoffWeight())));
			ae.addContent(XMLUtils.createElement("maxLandingWeight", String.valueOf(a.getMaxLandingWeight())));

			// Dump policy options/app names
			for (String appCode : a.getApps()) {
				AircraftPolicyOptions opts = a.getOptions(appCode);
				Element ape = XMLUtils.createElement("app", appCode);
				if (amsg.getShowPolicy()) {
					ape.setAttribute("seats", String.valueOf(opts.getSeats()));
					ape.setAttribute("range", String.valueOf(opts.getRange()));
					ape.setAttribute("toRunwayLength", String.valueOf(opts.getTakeoffRunwayLength()));
					ape.setAttribute("lndRunwayLength", String.valueOf(opts.getLandingRunwayLength()));
					if (opts.getETOPS() != ETOPS.INVALID) ape.setAttribute("etops", opts.getETOPS().name());
				} else if (appCode.equals(amsg.getSender().getAirlineCode())) {
					ae.setAttribute("seats", String.valueOf(opts.getSeats()));
					ae.setAttribute("etops", String.valueOf(opts.getETOPS() != ETOPS.INVALID));
					ae.addContent(XMLUtils.createElement("range", String.valueOf(opts.getRange())));
					ae.addContent(XMLUtils.createElement("toRunwayLength", String.valueOf(opts.getTakeoffRunwayLength())));
					ae.addContent(XMLUtils.createElement("lndRunwayLength", String.valueOf(opts.getLandingRunwayLength())));
				}

				ae.addContent(ape);
			}

			// Dump IATA codes
			a.getIATA().forEach(iataCode -> ae.addContent(XMLUtils.createElement("iata", iataCode, false)));

			// Get tank names/percentages
			Map<TankType, Collection<String>> tNames = a.getTankNames();
			Map<TankType, Integer> tPct = a.getTankPercent();
			ae.addContent(XMLUtils.createElement("pTanks", StringUtils.listConcat(tNames.get(TankType.PRIMARY), ",")));
			ae.addContent(XMLUtils.createElement("pPct", String.valueOf(tPct.get(TankType.PRIMARY))));
			ae.addContent(XMLUtils.createElement("sTanks", StringUtils.listConcat(tNames.get(TankType.SECONDARY), ",")));
			ae.addContent(XMLUtils.createElement("sPct", String.valueOf(tPct.get(TankType.SECONDARY))));
			ae.addContent(XMLUtils.createElement("oTanks", StringUtils.listConcat(tNames.get(TankType.OTHER), ",")));
			e.addContent(ae);
		}
		
		return pe;
	}
}