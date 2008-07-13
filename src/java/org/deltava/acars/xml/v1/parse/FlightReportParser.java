// Copyright 2005, 2006, 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import java.text.*;
import java.util.Date;

import org.jdom.Document;

import org.deltava.beans.*;
import org.deltava.beans.schedule.Airport;

import org.deltava.util.*;
import org.deltava.util.system.SystemData;

import org.deltava.acars.message.*;

import org.deltava.acars.util.ACARSHelper;
import org.deltava.acars.xml.XMLException;

/**
 * A parser for FlightReport elements.
 * @author Luke
 * @version 2.2
 * @since 1.0
 */

class FlightReportParser extends ElementParser {

	/**
	 * Convert an XML flight report element into a FlightReportMessage.
	 * @param e the XML element
	 * @return a FlightReportMessage
	 * @throws XMLException if a parse error occurs 
	 */
	public Message parse(org.jdom.Element e, Pilot user) throws XMLException {

		// Build the message bean
		FlightReportMessage msg = new FlightReportMessage(user);

		// Build the PIREP
		ACARSFlightReport afr = ACARSHelper.create(getChildText(e, "flightcode", "001"));
		try {
			afr.setLeg(StringUtils.parse(getChildText(e, "leg", "1"), 1));
			afr.setAttribute(FlightReport.ATTR_ACARS, true);
			afr.setDatabaseID(FlightReport.DBID_ACARS, Integer.parseInt(e.getChildTextTrim("flightID")));
			afr.setDatabaseID(FlightReport.DBID_PILOT, user.getID());
			afr.setRank(user.getRank());
			afr.setStatus(FlightReport.SUBMITTED);
			afr.setEquipmentType(getChildText(e, "eqType", "CRJ-200"));
			afr.setDate(new Date());
			afr.setSubmittedOn(afr.getDate());
			afr.setAirportD(getAirport(e.getChildTextTrim("airportD").toUpperCase()));
			afr.setAirportA(getAirport(e.getChildTextTrim("airportA").toUpperCase()));
			afr.setRemarks(e.getChildText("remarks"));
			afr.setFDE(getChildText(e, "fde", null));
			afr.setAircraftCode(getChildText(e, "code", null));

			// Check if it's a checkride
			afr.setAttribute(FlightReport.ATTR_CHECKRIDE, Boolean.valueOf(e.getChildTextTrim("checkRide")).booleanValue());

			// Get the online network
			String network = getChildText(e, "network", "Offline").toUpperCase();
			if (OnlineNetwork.VATSIM.equals(network))
				afr.setAttribute(FlightReport.ATTR_VATSIM, true);
			else if (OnlineNetwork.IVAO.equals(network))
				afr.setAttribute(FlightReport.ATTR_IVAO, true);
			else if (OnlineNetwork.FPI.equals(network))
				afr.setAttribute(FlightReport.ATTR_FPI, true);

			// Set the times
			final DateFormat dtf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
			try {
				afr.setStartTime(dtf.parse(e.getChildTextTrim("startTime")));
				afr.setTaxiTime(dtf.parse(e.getChildTextTrim("taxiOutTime")));
				afr.setTakeoffTime(dtf.parse(e.getChildTextTrim("takeoffTime")));
				afr.setLandingTime(dtf.parse(e.getChildTextTrim("landingTime")));
				afr.setEndTime(dtf.parse(e.getChildTextTrim("gateTime")));
			} catch (ParseException pex) {
				throw new XMLException("Invalid Date/Time - " + pex.getMessage(), pex);
			}

			// Set the weights/speeds
			afr.setTaxiFuel(StringUtils.parse(getChildText(e, "taxiFuel", "0"), 0));
			afr.setTaxiWeight(StringUtils.parse(getChildText(e, "taxiWeight", "1"), 0));
			afr.setTakeoffFuel(StringUtils.parse(getChildText(e, "takeoffFuel", "0"), 0));
			afr.setTakeoffWeight(StringUtils.parse(getChildText(e, "takeoffWeight", "1"), 0));
			afr.setTakeoffSpeed(StringUtils.parse(getChildText(e, "takeoffSpeed", "0"), 0));
			afr.setLandingFuel(StringUtils.parse(getChildText(e, "landingFuel", "0"), 0));
			afr.setLandingWeight(StringUtils.parse(getChildText(e, "landingWeight", "1"), 0));
			afr.setLandingSpeed(StringUtils.parse(getChildText(e, "landingSpeed", "0"), 0));
			afr.setLandingVSpeed(StringUtils.parse(getChildText(e, "landingVSpeed", "-1"), 0));
			afr.setGateFuel(StringUtils.parse(getChildText(e, "gateFuel", "0"), 0));
			afr.setGateWeight(StringUtils.parse(getChildText(e, "gateWeight", "1"), 0));

			// Set the Takeoff/Landing N1 values, but don't fail on invalid numeric values
			try {
				afr.setTakeoffN1(Double.parseDouble(getChildText(e, "takeoffN1", "0")));
				afr.setLandingN1(Double.parseDouble(getChildText(e, "landingN1", "0")));
			} catch (Exception ex) {
				throw new IllegalArgumentException("Invalid N1 - " + ex.getMessage(), ex);
			}

			// Load the 0X/1X/2X/4X times
			afr.setTime(0, StringUtils.parse(getChildText(e, "time0X", "0"), 0));
			afr.setTime(1, StringUtils.parse(getChildText(e, "time1X", "0"), 0));
			afr.setTime(2, StringUtils.parse(getChildText(e, "time2X", "0"), 0));
			afr.setTime(4, StringUtils.parse(getChildText(e, "time4X", "0"), 0));

			// Save the PIREP
			msg.setPIREP(afr);
		} catch (Exception ex) {
			log.error("Error submitting PIREP from " + user.getPilotCode());
			Document doc = new Document();
			doc.setRootElement(e);
			log.error(XMLUtils.format(doc, "UTF-8"), ex);
			throw new XMLException(ex.getMessage());
		}

		// Return the message
		return msg;
	}
	
	/**
	 * Helper method to load an airport.
	 */
	private Airport getAirport(String code) throws XMLException {
		Airport a = SystemData.getAirport(code);
		if (a == null)
			throw new XMLException("Invalid Airport Code - " + code);

		return a;
	}
}