// Copyright 2005, 2006, 2008, 2009, 2010, 2011, 2012, 2014, 2015, 2016, 2017, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import java.time.*;
import java.time.format.DateTimeFormatterBuilder;

import org.apache.log4j.Logger;

import org.deltava.beans.*;
import org.deltava.beans.flight.*;
import org.deltava.beans.schedule.*;

import org.deltava.util.*;
import org.deltava.acars.message.*;

import org.deltava.acars.util.ACARSHelper;
import org.deltava.acars.xml.*;

/**
 * A parser for FlightReport elements.
 * @author Luke
 * @version 8.1
 * @since 1.0
 */

class FlightReportParser extends XMLElementParser<FlightReportMessage> {
	
	private static final Logger log = Logger.getLogger(FlightReportParser.class);
	
	/**
	 * Convert an XML flight report element into a FlightReportMessage.
	 * @param e the XML element
	 * @return a FlightReportMessage
	 * @throws XMLException if a parse error occurs 
	 */
	@Override
	public FlightReportMessage parse(org.jdom2.Element e, Pilot user) throws XMLException {

		// Build the message bean
		FlightReportMessage msg = new FlightReportMessage(user);

		// Build the PIREP
		ACARSFlightReport afr = ACARSHelper.create(getChildText(e, "flightcode", "001"));
		afr.setLeg(StringUtils.parse(getChildText(e, "leg", "1"), 1));
		afr.setAttribute(FlightReport.ATTR_ACARS, true);
		afr.setAttribute(FlightReport.ATTR_DIVERT, Boolean.valueOf(getChildText(e, "isDivert", "false")).booleanValue());
		afr.setDatabaseID(DatabaseID.ACARS, StringUtils.parse(e.getChildTextTrim("flightID"), 0));
		afr.setStatus(FlightStatus.SUBMITTED);
		afr.setEquipmentType(getChildText(e, "eqType", "CRJ-200"));
		afr.setDate(Instant.now());
		afr.setSubmittedOn(afr.getDate());
		afr.setHasReload(Boolean.valueOf(getChildText(e, "hasRestore", "false")).booleanValue());
		afr.setAirportD(getAirport(e.getChildTextTrim("airportD")));
		afr.setAirportA(getAirport(e.getChildTextTrim("airportA")));
		afr.setRemarks(e.getChildText("remarks"));
		afr.setFDE(getChildText(e, "fde", null));
		afr.setAircraftCode(getChildText(e, "code", null));
		afr.setNetwork(OnlineNetwork.fromName(getChildText(e, "network", null)));
		
		// Check for SDK and load data (this is really v2, but no sense making a new parser for a three element delta)
		afr.setSDK(getChildText(e, "sdk", null));
		String lf = getChildText(e, "loadFactor", "0");
		afr.setLoadFactor(StringUtils.parse(lf, 0.0));
		if (Double.isNaN(afr.getLoadFactor())) {
			log.warn("Invalid load factor from " + user.getPilotCode() + " - " + lf);
			afr.setLoadFactor(0);
		}
			
		// Check for dispatch data
		msg.setDispatcherID(StringUtils.parse(getChildText(e, "dispatcherID", "0"), 0));
		msg.setRouteID(StringUtils.parse(getChildText(e, "dispatchRouteID", "0"), 0));

		// Check if it's a checkride
		afr.setAttribute(FlightReport.ATTR_CHECKRIDE, Boolean.valueOf(e.getChildTextTrim("checkRide")).booleanValue());

		// Set the times
		try {
			final DateTimeFormatterBuilder dfb = new DateTimeFormatterBuilder().appendPattern("MM/dd/yyyy HH:mm:ss");
			afr.setStartTime(LocalDateTime.parse(e.getChildTextTrim("startTime"), dfb.toFormatter()).toInstant(ZoneOffset.UTC));
			afr.setTaxiTime(LocalDateTime.parse(e.getChildTextTrim("taxiOutTime"), dfb.toFormatter()).toInstant(ZoneOffset.UTC));
			afr.setTakeoffTime(LocalDateTime.parse(e.getChildTextTrim("takeoffTime"), dfb.toFormatter()).toInstant(ZoneOffset.UTC));
			afr.setLandingTime(LocalDateTime.parse(e.getChildTextTrim("landingTime"), dfb.toFormatter()).toInstant(ZoneOffset.UTC));
			afr.setEndTime(LocalDateTime.parse(e.getChildTextTrim("gateTime"), dfb.toFormatter()).toInstant(ZoneOffset.UTC));
		} catch (Exception pex) {
			throw new XMLException("Invalid Date/Time - " + pex.getMessage(), pex);
		}

		// Set the weights/speeds
		afr.setPaxWeight(StringUtils.parse(getChildText(e, "paxWeight", "0"), 0));
		afr.setCargoWeight(StringUtils.parse(getChildText(e, "cargoWeight", "0"), 0));
		afr.setTaxiFuel(StringUtils.parse(getChildText(e, "taxiFuel", "0"), 0));
		afr.setTaxiWeight(StringUtils.parse(getChildText(e, "taxiWeight", "1"), 0));
		afr.setTakeoffFuel(StringUtils.parse(getChildText(e, "takeoffFuel", "0"), 0));
		afr.setTakeoffWeight(StringUtils.parse(getChildText(e, "takeoffWeight", "1"), 0));
		afr.setTakeoffSpeed(StringUtils.parse(getChildText(e, "takeoffSpeed", "0"), 0));
		afr.setTakeoffN1(StringUtils.parse(getChildText(e, "takeoffN1", "0.0"), 0.0d));
		afr.setLandingFuel(StringUtils.parse(getChildText(e, "landingFuel", "0"), 0));
		afr.setLandingWeight(StringUtils.parse(getChildText(e, "landingWeight", "1"), 0));
		afr.setLandingSpeed(StringUtils.parse(getChildText(e, "landingSpeed", "0"), 0));
		afr.setLandingVSpeed(StringUtils.parse(getChildText(e, "landingVSpeed", "-1"), 0));
		afr.setLandingG(StringUtils.parse(getChildText(e, "landingG", "0.0"), 0.0d));
		afr.setLandingN1(StringUtils.parse(getChildText(e, "landingN1", "0.0"), 0.0d));
		afr.setLandingCategory(ILSCategory.get(getChildText(e, "landingCat", "")));
		afr.setGateFuel(StringUtils.parse(getChildText(e, "gateFuel", "0"), 0));
		afr.setGateWeight(StringUtils.parse(getChildText(e, "gateWeight", "1"), 0));
			
		// Get the takeoff data
		afr.setTakeoffHeading(StringUtils.parse(getChildText(e, "takeoffHeading", "-1"), -1));
		if (afr.getTakeoffHeading() > -1) {
			double lat = StringUtils.parse(getChildText(e, "takeoffLat", "0.0"), 0.0d);
			double lng = StringUtils.parse(getChildText(e, "takeoffLng", "0.0"), 0.0d);
			afr.setTakeoffLocation(new GeoPosition(lat, lng, StringUtils.parse(getChildText(e, "takeoffAlt", "0"), 0)));
		}
			
		// Get the landing data
		afr.setLandingHeading(StringUtils.parse(getChildText(e, "landingHeading", "-1"), -1));
		if (afr.getLandingHeading() > -1) {
			double lat = StringUtils.parse(getChildText(e, "landingLat", "0.0"), 0.0d);
			double lng = StringUtils.parse(getChildText(e, "landingLng", "0.0"), 0.0d);	
			afr.setLandingLocation(new GeoPosition(lat, lng, StringUtils.parse(getChildText(e, "landingAlt", "0"), 0)));
		}

		// Load the 0X/1X/2X/4X times
		afr.setTime(0, StringUtils.parse(getChildText(e, "time0X", "0"), 0));
		afr.setTime(1, StringUtils.parse(getChildText(e, "time1X", "0"), 0));
		afr.setTime(2, StringUtils.parse(getChildText(e, "time2X", "0"), 0));
		afr.setTime(4, StringUtils.parse(getChildText(e, "time4X", "0"), 0));

		// Save the PIREP and return
		msg.setPIREP(afr);
		return msg;
	}
}