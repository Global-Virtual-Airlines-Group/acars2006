// Copyright 2005, 2006, 2008, 2009, 2010, 2011, 2012, 2014, 2015, 2016, 2017, 2018, 2019, 2020, 2021, 2022, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import java.time.*;
import java.time.format.*;
import java.time.temporal.ChronoField;

import org.jdom2.Element;
import org.apache.logging.log4j.*;

import org.deltava.beans.*;
import org.deltava.beans.acars.DispatchType;
import org.deltava.beans.flight.*;
import org.deltava.beans.schedule.*;

import org.deltava.util.*;

import org.deltava.acars.message.*;
import org.deltava.acars.xml.*;

/**
 * A parser for v2 FlightReport elements.
 * @author Luke
 * @version 10.5
 * @since 10.5
 */

class FlightReportParser extends XMLElementParser<FlightReportMessage> {
	
	private static final Logger log = LogManager.getLogger(FlightReportParser.class);
	
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
		Flight fc = FlightCodeParser.parse(getChildText(e, "flightcode", "1"), user.getAirlineCode());
		ACARSFlightReport afr = new ACARSFlightReport(fc.getAirline(), fc.getFlightNumber(), StringUtils.parse(getChildText(e, "leg", "1"), 1));
		afr.setAttribute(FlightReport.ATTR_ACARS, true);
		afr.setAttribute(FlightReport.ATTR_DIVERT, Boolean.parseBoolean(getChildText(e, "isDivert", "false")));
		afr.setDatabaseID(DatabaseID.ACARS, StringUtils.parse(e.getChildTextTrim("flightID"), 0));
		afr.setDatabaseID(DatabaseID.DISPATCH, StringUtils.parse(e.getChildTextTrim("dispatchLogID"), 0));
		afr.setStatus(FlightStatus.SUBMITTED);
		afr.setEquipmentType(getChildText(e, "eqType", "CRJ-200"));
		afr.setDate(Instant.now());
		afr.setSubmittedOn(afr.getDate());
		afr.setRestoreCount(StringUtils.parse(e.getChildTextTrim("restoreCount"), 0));
		afr.setAirportD(getAirport(e.getChildTextTrim("airportD")));
		afr.setAirportA(getAirport(e.getChildTextTrim("airportA")));
		afr.setRemarks(e.getChildText("remarks"));
		afr.setFDE(getChildText(e, "fde", null));
		afr.setCapabilities(StringUtils.parse(getChildText(e, "capabilities", "0"), 0, true));
		afr.setAircraftCode(getChildText(e, "code", null));
		afr.setAuthor(getChildText(e, "author", null));
		afr.setAircraftPath(getChildText(e, "acPath", null));
		afr.setNetwork(EnumUtils.parse(OnlineNetwork.class, getChildText(e, "network", null), null));
		afr.setTailCode(getChildText(e, "tailCode", null));
		
		// Check for SDK and load data
		afr.setSDK(getChildText(e, "sdk", ACARSFlightReport.GENERIC_SDK));
		afr.setPassengers(StringUtils.parse(getChildText(e, "pax", "0"), 0));
		msg.setCustomCabinSize(Boolean.parseBoolean(getChildText(e, "customCabin", "false")));
		msg.setPaxWeight(StringUtils.parse(getChildText(e, "passengerWeight", String.valueOf(FlightReportMessage.DEFAULT_PAX_WEIGHT)), FlightReportMessage.DEFAULT_PAX_WEIGHT));
		String lf = getChildText(e, "loadFactor", "0");
		afr.setLoadFactor(StringUtils.parse(lf, 0.0));
		if (Double.isNaN(afr.getLoadFactor())) {
			log.warn("Invalid load factor from " + user.getPilotCode() + " - " + lf);
			afr.setLoadFactor(0);
		}
			
		// Check for dispatch data
		msg.setDispatcherID(StringUtils.parse(getChildText(e, "dispatcherID", "0"), 0));
		msg.setRouteID(StringUtils.parse(getChildText(e, "dispatchRouteID", "0"), 0));
		if (msg.getDispatcher() != DispatchType.DISPATCH)
			msg.setDispatcher(EnumUtils.parse(DispatchType.class, getChildText(e, "dispatcher", "none"), DispatchType.NONE));

		// Check if it's a checkride
		afr.setAttribute(FlightReport.ATTR_CHECKRIDE, Boolean.parseBoolean(e.getChildTextTrim("checkRide")));

		// Set the times
		try {
			final DateTimeFormatterBuilder dfb = new DateTimeFormatterBuilder().appendPattern("MM/dd/yyyy HH:mm:ss");
			afr.setStartTime(LocalDateTime.parse(e.getChildTextTrim("startTime"), dfb.toFormatter()).toInstant(ZoneOffset.UTC));
			afr.setTaxiTime(LocalDateTime.parse(e.getChildTextTrim("taxiOutTime"), dfb.toFormatter()).toInstant(ZoneOffset.UTC));
			afr.setTakeoffTime(LocalDateTime.parse(e.getChildTextTrim("takeoffTime"), dfb.toFormatter()).toInstant(ZoneOffset.UTC));
			afr.setLandingTime(LocalDateTime.parse(e.getChildTextTrim("landingTime"), dfb.toFormatter()).toInstant(ZoneOffset.UTC));
			afr.setEndTime(LocalDateTime.parse(e.getChildTextTrim("gateTime"), dfb.toFormatter()).toInstant(ZoneOffset.UTC));
			if (XMLUtils.hasElement(e, "tocTime"))
				afr.setTOCTime(LocalDateTime.parse(e.getChildTextTrim("tocTime"), dfb.toFormatter()).toInstant(ZoneOffset.UTC));
			if (XMLUtils.hasElement(e, "todTime"))
				afr.setTODTime(LocalDateTime.parse(e.getChildTextTrim("todTime"), dfb.toFormatter()).toInstant(ZoneOffset.UTC));
		} catch (Exception pex) {
			throw new XMLException("Invalid Date/Time - " + pex.getMessage(), pex);
		}
		
		// Check for sim times
		if (e.getChild("startSimTime") != null) {
			try {
				afr.setDepartureTime(StringUtils.parseInstant(e.getChildTextTrim("startSimTime"), "MM/dd/yyyy HH:mm:ss"));
				afr.setArrivalTime(StringUtils.parseInstant(e.getChildTextTrim("gateSimTime"), "MM/dd/yyyy HH:mm:ss"));
			} catch (Exception ex) {
				log.warn("Error parsing sim time - " + ex.getMessage());
			}
		}
		
		// Get start position
		if (XMLUtils.hasElement(e, "startLat")) {
			double lat = StringUtils.parse(getChildText(e, "startLat", "0.0"), 0.0d);
			double lng = StringUtils.parse(getChildText(e, "startLng", "0.0"), 0.0d);
			afr.setStartLocation(new GeoPosition(lat, lng, Math.min(24000, StringUtils.parse(getChildText(e, "startAlt", "0"), 0))));
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
		afr.setLandingCategory(EnumUtils.parse(ILSCategory.class, getChildText(e, "landingCat", ""), ILSCategory.NONE));
		afr.setGateFuel(StringUtils.parse(getChildText(e, "gateFuel", "0"), 0));
		afr.setGateWeight(StringUtils.parse(getChildText(e, "gateWeight", "1"), 0));
			
		// Get the takeoff data
		afr.setTakeoffHeading(StringUtils.parse(getChildText(e, "takeoffHeading", "-1"), -1));
		if (afr.getTakeoffHeading() > -1) {
			double lat = StringUtils.parse(getChildText(e, "takeoffLat", "0.0"), 0.0d);
			double lng = StringUtils.parse(getChildText(e, "takeoffLng", "0.0"), 0.0d);
			afr.setTakeoffLocation(new GeoPosition(lat, lng, Math.min(24000, StringUtils.parse(getChildText(e, "takeoffAlt", "0"), 0))));
		}
			
		// Get the landing data
		afr.setLandingHeading(StringUtils.parse(getChildText(e, "landingHeading", "-1"), -1));
		if (afr.getLandingHeading() > -1) {
			double lat = StringUtils.parse(getChildText(e, "landingLat", "0.0"), 0.0d);
			double lng = StringUtils.parse(getChildText(e, "landingLng", "0.0"), 0.0d);	
			afr.setLandingLocation(new GeoPosition(lat, lng, Math.min(24000, StringUtils.parse(getChildText(e, "landingAlt", "0"), 0))));
		}
		
		// Get end position
		if (XMLUtils.hasElement(e, "shutdownLat")) {
			double lat = StringUtils.parse(getChildText(e, "shutdownLat", "0.0"), 0.0d);
			double lng = StringUtils.parse(getChildText(e, "shutdownLng", "0.0"), 0.0d);
			afr.setEndLocation(new GeoPosition(lat, lng, Math.min(24000, StringUtils.parse(getChildText(e, "shutdownAlt", "0"), 0))));
		}

		// Load the 0X/1X/2X/4X times
		afr.setTime(0, StringUtils.parse(getChildText(e, "time0X", "0"), 0));
		afr.setTime(1, StringUtils.parse(getChildText(e, "time1X", "0"), 0));
		afr.setTime(2, StringUtils.parse(getChildText(e, "time2X", "0"), 0));
		afr.setTime(4, StringUtils.parse(getChildText(e, "time4X", "0"), 0));
		afr.setBoardTime(StringUtils.parse(getChildText(e, "timeBoard", "0"), 0));
		afr.setDeboardTime(StringUtils.parse(getChildText(e, "timeDeboard", "0"), 0));
		afr.setOnlineTime(StringUtils.parse(getChildText(e, "timeOnline", "0"), 0));
		
		// Parse status messages
		if (XMLUtils.hasElement(e, "msgs")) {
			DateTimeFormatter mdtf = new DateTimeFormatterBuilder().appendPattern("MM/dd/yyyy HH:mm:ss").appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true).toFormatter();
			for (Element me : e.getChild("msgs").getChildren()) {
				HistoryType ht = EnumUtils.parse(HistoryType.class, me.getAttributeValue("type"), HistoryType.USER);
				int userID = (ht == HistoryType.USER) ? StringUtils.parse(me.getAttributeValue("userID"), user.getID()) : 0;
				Instant dt = LocalDateTime.parse(me.getAttributeValue("time"), mdtf).toInstant(ZoneOffset.UTC);
				afr.addStatusUpdate(new FlightHistoryEntry(0, ht, userID, dt, me.getText()));
			}
		}

		// Save the PIREP and return
		msg.setPIREP(afr);
		return msg;
	}
}