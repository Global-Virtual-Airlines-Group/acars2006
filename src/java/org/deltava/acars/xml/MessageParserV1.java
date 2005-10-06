// Copyright 2004, 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.xml;

import java.util.*;
import java.text.*;

import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import org.apache.log4j.Logger;

import org.deltava.beans.*;
import org.deltava.acars.message.*;
import org.deltava.acars.util.ACARSHelper;

import org.deltava.beans.schedule.Airport;

import org.deltava.util.system.SystemData;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

class MessageParserV1 implements MessageParser {

	private static final Logger log = Logger.getLogger(MessageParserV1.class);

	private final DateFormat _dtf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

	// ACARS protcol version
	private static final int PROTOCOL_VERSION = 1;

	private Element _el;
	private long _timeStamp;
	private Pilot _user;

	MessageParserV1() {
		super();
	}

	MessageParserV1(Element e) {
		super();
		_el = e;
	}

	public void setElement(Element e) {
		_el = e;
	}

	public void setTime(long ts) {
		_timeStamp = ts;
	}

	public void setUser(Pilot userInfo) {
		_user = userInfo;
	}

	public int getProtocolVersion() {
		return PROTOCOL_VERSION;
	}

	private String getChildText(String childName, String defaultValue) {
		return getChildText(_el, childName, defaultValue);
	}

	private String getChildText(Element e, String childName, String defaultValue) {
		String tmp = e.getChildTextTrim(childName);
		return (tmp == null) ? defaultValue : tmp;
	}
	
	private Airport getAirport(String code) throws XMLException {
		Airport a = SystemData.getAirport(code);
		if (a == null)
			throw new XMLException("Invalid Airport Code - " + code);
		
		return a;
	}

	public Message parse(int msgType) throws XMLException {

		// Depending on the message type, call a parse method
		switch (msgType) {
			case Message.MSG_POSITION:
				return parsePosition(_el);

			case Message.MSG_INFO:
				return parseInfo(_el);

			case Message.MSG_TEXT:
				return parseText();

			case Message.MSG_AUTH:
				return parseAuth();

			case Message.MSG_DIAG:
				return parseDiag();

			case Message.MSG_ACK:
				return parseAck();

			case Message.MSG_DATAREQ:
				return parseDataReq();

			case Message.MSG_PIREP:
				return parsePIREP();

			case Message.MSG_ENDFLIGHT:
				return new EndFlightMessage(_user);

			case Message.MSG_PING:
				return new PINGMessage(_user);

			case Message.MSG_QUIT:
				return new QuitMessage(_user);

			default:
				throw new XMLException("Invalid message type - " + msgType);
		}
	}

	private Message parseAuth() throws XMLException {

		// Get the user ID and password and validate
		String userID = getChildText("user", null);
		String pwd = getChildText("password", null);
		if ((userID == null) || (pwd == null))
			throw new XMLException("Missing userID/password");
		
		// Create the bean and use this protocol version for responses
		AuthenticateMessage msg = new AuthenticateMessage(userID, pwd);
		msg.setProtocolVersion(PROTOCOL_VERSION);
		try {
			msg.setClientBuild(Integer.parseInt(getChildText("build", "0")));	
		} catch (NumberFormatException nfe) {
			throw new XMLException("Invalid Build Number - " + getChildText("build", ""), nfe);
		}

		// Return the bean
		return msg;
	}

	private Message parsePosition(Element e) throws XMLException {

		// Create the bean
		PositionMessage msg = new PositionMessage(_user);
		msg.setTime(_timeStamp);

		// Parse the date
		try {
			msg.setDate(_dtf.parse(getChildText(e, "date", "")));
		} catch (Exception ex) {
			msg.setDate(new Date(_timeStamp));
		}

		// Get the basic information
		try {
			msg.setHeading(Integer.parseInt(getChildText(e, "hdg", "0")));
			msg.setLatitude(Double.parseDouble(getChildText(e, "lat", "0")));
			msg.setLongitude(Double.parseDouble(getChildText(e, "lon", "0")));
			msg.setAltitude(Integer.parseInt(getChildText(e, "msl", "0")));
			msg.setRadarAltitude(Integer.parseInt(getChildText(e, "agl", "0")));
			msg.setAspeed(Integer.parseInt(getChildText(e, "aSpeed", "0")));
			msg.setGspeed(Integer.parseInt(getChildText(e, "gSpeed", "0")));
			msg.setVspeed(Integer.parseInt(getChildText(e, "vSpeed", "0")));
			msg.setMach(Double.parseDouble(getChildText(e, "mach", "0")));
			msg.setFuelRemaining(Integer.parseInt(getChildText(e, "fuel", "0")));
			msg.setFlaps(Integer.parseInt(getChildText(e, "flaps", "0")));
			msg.setFlags(Integer.parseInt(getChildText(e, "flags", "0")));
			msg.setN1(Double.parseDouble(getChildText(e, "n1", "0")));
			msg.setN2(Double.parseDouble(getChildText(e, "n2", "0")));
			msg.setPhase(getChildText(e, "phase", PositionMessage.FLIGHT_PHASES[PositionMessage.PHASE_UNKNOWN]));
			msg.setSimRate(Integer.parseInt(getChildText(e, "simrate", "256")));
			msg.setNoFlood(Boolean.valueOf(getChildText(e, "noFlood", "false")).booleanValue());
		} catch (Exception ex) {
			throw new XMLException("Error parsing Position data - " + ex.getMessage(), ex);
		}

		// Return the bean
		return msg;
	}

	private Message parseText() throws XMLException {

		// Get the message text
		String msgText = getChildText("text", null);
		if (msgText == null)
			throw new XMLException("No Message Text");

		// Create the bean and set the receipients
		TextMessage msg = new TextMessage(_user, msgText);
		msg.setTime(_timeStamp);
		msg.setRecipient(getChildText("to", null));

		// Return the message bean
		return msg;
	}

	private Message parseAck() throws XMLException {

		// Get the id of the message we are acking
		String txtID = _el.getAttributeValue("id");
		if (txtID == null)
			throw new XMLException("No Message ID to Acknowledge");

		// Create the bean and return it
		try {
			return new AcknowledgeMessage(_user, Long.parseLong(txtID, 16));
		} catch (Exception e) {
			throw new XMLException("Cannot format ID " + txtID + " - " + e.getMessage(), e);
		}
	}

	private Message parseInfo(Element e) throws XMLException {

		// Create the bean
		InfoMessage msg = new InfoMessage(_user);
		msg.setTime(_timeStamp);

		// Check if we're loading an existing flight ID
		try {
			msg.setFlightID(Integer.parseInt(getChildText(e, "flight_id", "0")));
		} catch (NumberFormatException nfe) {
			msg.setFlightID(0);
		}

		// Parse the start date/time
		try {
			msg.setStartTime(_dtf.parse(getChildText(e, "startTime", "")));
		} catch (Exception ex) {
			msg.setStartTime(new Date(_timeStamp));
		}

		// Load the bean
		msg.setEquipmentType(getChildText(e, "equipment", "UNKNOWN"));
		msg.setFlightCode(getChildText(e, "flight_num", "???"));
		msg.setAltitude(getChildText(e, "cruise_alt", null));
		msg.setWaypoints(getChildText(e, "route", "DIRECT"));
		msg.setComments(getChildText(e, "remarks", null));
		msg.setFSVersion(Integer.parseInt(getChildText(e, "fs_ver", "2004")));
		msg.setAirportD(getAirport(getChildText(e, "airportD", null)));
		msg.setAirportA(getAirport(getChildText(e, "airportA", null)));
		msg.setOffline(Boolean.valueOf(getChildText(e, "offline", "false")).booleanValue());
		msg.setComplete(Boolean.valueOf(getChildText("complete", "false")).booleanValue());
		return msg;
	}

	private Message parseDiag() {

		// Create the bean
		DiagnosticMessage msg = new DiagnosticMessage(_user);
		msg.setTime(_timeStamp);

		// Set the request type
		msg.setRequestType(getChildText("reqtype", ""));
		msg.setRequestData(getChildText("reqdata", null));
		return msg;
	}

	private Message parseDataReq() throws XMLException {

		// Get the request type and validate
		String rType = getChildText("reqtype", null);
		if (rType == null)
			throw new XMLException("Invalid Data Request Type");

		// Create the bean
		DataRequestMessage msg = new DataRequestMessage(_user, rType);
		msg.setTime(_timeStamp);

		// Load the flags
		Element flagsE = _el.getChild("flags");
		if (flagsE != null) {
			for (Iterator i = flagsE.getChildren().iterator(); i.hasNext();) {
				Element e = (Element) i.next();
				msg.addFlag(e.getName(), e.getTextTrim());
			}
		}

		// Return the bean
		return msg;
	}

	private Message parsePIREP() throws XMLException {

		// Build the message bean
		FlightReportMessage msg = new FlightReportMessage(_user);

		// Build the PIREP
		ACARSFlightReport afr = ACARSHelper.create(getChildText("flightcode", "001"));
		try {
			afr.setLeg(Integer.parseInt(getChildText("leg", "1")));
			afr.setAttribute(FlightReport.ATTR_ACARS, true);
			afr.setDatabaseID(FlightReport.DBID_ACARS, Integer.parseInt(_el.getChildTextTrim("flightID")));
			afr.setDatabaseID(FlightReport.DBID_PILOT, _user.getID());
			afr.setRank(_user.getRank());
			afr.setStatus(FlightReport.SUBMITTED);
			afr.setEquipmentType(_el.getChildTextTrim("eqType"));
			afr.setDate(new Date());
			afr.setCreatedOn(afr.getDate());
			afr.setSubmittedOn(afr.getDate());
			afr.setAirportD(getAirport(_el.getChildTextTrim("airportD").toUpperCase()));
			afr.setAirportA(getAirport(_el.getChildTextTrim("airportA").toUpperCase()));
			afr.setRemarks(_el.getChildText("remarks"));

			// Check if it's a checkride
			boolean isCR = Boolean.valueOf(_el.getChildTextTrim("checkRide")).booleanValue();
			afr.setAttribute(FlightReport.ATTR_CHECKRIDE, isCR);

			// Get the online network
			String network = getChildText("network", "Offline").toUpperCase();
			if ("VATSIM".equals(network)) {
				afr.setAttribute(FlightReport.ATTR_VATSIM, true);
			} else if ("IVAO".equals(network)) {
				afr.setAttribute(FlightReport.ATTR_IVAO, true);
			} else if ("FPI".equals(network)) {
				afr.setAttribute(FlightReport.ATTR_FPI, true);
			}

			// Set the times
			try {
				afr.setStartTime(_dtf.parse(_el.getChildTextTrim("startTime")));
				afr.setTaxiTime(_dtf.parse(_el.getChildTextTrim("taxiOutTime")));
				afr.setTakeoffTime(_dtf.parse(_el.getChildTextTrim("takeoffTime")));
				afr.setLandingTime(_dtf.parse(_el.getChildTextTrim("landingTime")));
				afr.setEndTime(_dtf.parse(_el.getChildTextTrim("gateTime")));
			} catch (ParseException pex) {
				throw new XMLException("Invalid Date/Time - " + pex.getMessage(), pex);
			}

			// Set the weights/speeds
			try {
				afr.setTaxiFuel(Integer.parseInt(getChildText("taxiFuel", "0")));
				afr.setTaxiWeight(Integer.parseInt(getChildText("taxiWeight", "1")));
				afr.setTakeoffFuel(Integer.parseInt(getChildText("takeoffFuel", "0")));
				afr.setTakeoffWeight(Integer.parseInt(getChildText("takeoffWeight", "1")));
				afr.setTakeoffN1(Double.parseDouble(getChildText("takeoffN1", "0")));
				afr.setTakeoffSpeed(Integer.parseInt(getChildText("takeoffSpeed", "0")));
				afr.setLandingFuel(Integer.parseInt(getChildText("landingFuel", "0")));
				afr.setLandingWeight(Integer.parseInt(getChildText("landingWeight", "1")));
				afr.setLandingN1(Double.parseDouble(getChildText("landingN1", "0")));
				afr.setLandingSpeed(Integer.parseInt(getChildText("landingSpeed", "0")));
				afr.setLandingVSpeed(Integer.parseInt(getChildText("landingVSpeed", "-1")));
				afr.setGateFuel(Integer.parseInt(getChildText("gateFuel", "0")));
				afr.setGateWeight(Integer.parseInt(getChildText("gateWeight", "1")));
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException("Invalid Weight/Speed - " + nfe.getMessage());
			}

			// Load the 1X/2X/4X times
			try {
				afr.setTime1X(Integer.parseInt(getChildText("time1X", "0")) * 60);
				afr.setTime2X(Integer.parseInt(getChildText("time2X", "0")) * 60);
				afr.setTime4X(Integer.parseInt(getChildText("time4X", "0")) * 60);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException("Invalid time - " + nfe.getMessage());
			}

			// Save the PIREP
			msg.setPIREP(afr);
		} catch (Exception e) {
			XMLOutputter xmlOut = new XMLOutputter(Format.getPrettyFormat());
			log.error(xmlOut.outputString(_el));
			throw new XMLException(e.getMessage());
		}

		// Return the message
		return msg;
	}
}