// Copyright 2006, 2008, 2009, 2012, 2017, 2021, 2022, 2023, 2024 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

import org.jdom2.Element;

import org.deltava.acars.beans.DraftFlightPackage;
import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.DraftPIREPMessage;

import org.deltava.beans.flight.*;
import org.deltava.beans.simbrief.*;
import org.deltava.beans.schedule.Airport;

import org.deltava.util.*;

/**
 * An XML Formatter for Draft Flight Report data messages.
 * @author Luke
 * @version 11.2
 * @since 1.0
 */

class DraftFlightFormatter extends ElementFormatter {

	/**
	 * Formats a DraftPIREPMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {

		// Cast the message
		DraftPIREPMessage dfmsg = (DraftPIREPMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "pireps");
		for (DraftFlightPackage dfp : dfmsg.getResponse()) {
			Element fe = new Element("pirep");
			DraftFlightReport fr = dfp.getFlightReport();
			fe.setAttribute("id", StringUtils.formatHex(fr.getID()));
			fe.setAttribute("airline", fr.getAirline().getCode());
			fe.setAttribute("number", StringUtils.format(fr.getFlightNumber(), "#000"));
			fe.setAttribute("leg", String.valueOf(fr.getLeg()));
			fe.setAttribute("assign", String.valueOf(fr.getDatabaseID(DatabaseID.ASSIGN) != 0));
			fe.setAttribute("isDivert", String.valueOf(fr.hasAttribute(FlightReport.ATTR_DIVERT)));
			fe.addContent(XMLUtils.createElement("eqType", fr.getEquipmentType()));
			fe.addContent(XMLUtils.createElement("airportA", fr.getAirportA().getICAO()));
			fe.addContent(XMLUtils.createElement("airportD", fr.getAirportD().getICAO()));
			fe.addContent(XMLUtils.createElement("remarks", fr.getRemarks(), true));
			fe.addContent(XMLUtils.createElement("comments", fr.getComments(), true));
			fe.addContent(XMLUtils.createElement("pax", String.valueOf(fr.getPassengers())));
			fe.addContent(XMLUtils.createElement("tourID", String.valueOf(fr.getDatabaseID(DatabaseID.TOUR))));
			if (fr.getNetwork() != null)
				fe.addContent(XMLUtils.createElement("network", fr.getNetwork().toString()));
			if (!StringUtils.isEmpty(fr.getRoute()))
				fe.addContent(XMLUtils.createElement("route", fr.getRoute(), true));
			
			// Add SimBrief data if present
			if (fr.hasAttribute(FlightReport.ATTR_SIMBRIEF) && (dfp.getPackage() != null)) {
				BriefingPackage pkg = dfp.getPackage();
				fe.setAttribute("isSimBrief", "true");
				fe.setAttribute("created", String.valueOf(pkg.getCreatedOn().toEpochMilli()));
				fe.addContent(XMLUtils.createElement("createdOn", StringUtils.format(pkg.getCreatedOn(), "MM/dd/yyyy HH:mm:ss")));
				fe.addContent(XMLUtils.createElement("airac", String.valueOf(pkg.getAIRAC())));
				XMLUtils.addIfPresent(fe, XMLUtils.createIfPresent("runwayD", pkg.getRunwayD()));
				XMLUtils.addIfPresent(fe, XMLUtils.createIfPresent("runwayA", pkg.getRunwayA()));
				XMLUtils.addIfPresent(fe, XMLUtils.createIfPresent("cruiseAlt", pkg.getCruiseAltitude()));
				fe.addContent(XMLUtils.createElement("baseFPURL", pkg.getBasePlanURL()));
				fe.addContent(XMLUtils.createElement("taxiFuel", String.valueOf(pkg.getTaxiFuel())));
				fe.addContent(XMLUtils.createElement("baseFuel", String.valueOf(pkg.getBaseFuel())));
				fe.addContent(XMLUtils.createElement("enrouteFuel", String.valueOf(pkg.getEnrouteFuel())));
				fe.addContent(XMLUtils.createElement("alternateFuel", String.valueOf(pkg.getAlternateFuel())));
				fe.addContent(XMLUtils.createElement("pax", String.valueOf(pkg.getPax())));
				fe.addContent(XMLUtils.createElement("bagWeight", String.valueOf(pkg.getBaggageWeight())));
				fe.addContent(XMLUtils.createElement("cargoWeight", String.valueOf(pkg.getCargoWeight())));
				for (Airport a : pkg.getAlternates())
					fe.addContent(formatAirport(a, "alternate"));
				
				// Load flightplans
				for (FlightPlan fp : pkg.getFlightPlans()) {
					Element fpe = new Element("flightplan");
					fpe.setAttribute("name", fp.getFileName());
					fpe.setAttribute("type", fp.getType());
					fe.addContent(fpe);
				}
			}
			
			// Add scheduled departure/arrival times and gates
			XMLUtils.addIfPresent(fe, XMLUtils.createIfPresent("gateD", fr.getGateD()));
			XMLUtils.addIfPresent(fe, XMLUtils.createIfPresent("gateA", fr.getGateA()));
			if (fr.getTimeD() != null)
				fe.addContent(XMLUtils.createElement("timeD", StringUtils.format(fr.getTimeD(), "HH:mm")));
			if (fr.getTimeA() != null)
				fe.addContent(XMLUtils.createElement("timeA", StringUtils.format(fr.getTimeA(), "HH:mm")));
			
			e.addContent(fe);
		}
		
		return pe;
	}
}