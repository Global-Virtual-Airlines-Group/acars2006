// Copyright 2004, 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.xml;

import java.util.*;
import java.text.*;

import org.jdom.Element;

import org.deltava.beans.*;
import org.deltava.acars.message.*;
import org.deltava.acars.util.ACARSHelper;

import org.deltava.util.system.SystemData;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

class MessageParserV1 implements MessageParser {
   
   private final DateFormat _dtf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");

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
	   return getChildText(childName, defaultValue);
	}
	
	private String getChildText(Element e, String childName, String defaultValue) {
	   String tmp = e.getChildTextTrim(childName);
		return (tmp == null) ? defaultValue : tmp;
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

		// Get any requested connection ID
		msg.setRequestedID(getChildText("connection", "0"));

		// Return the bean
		return msg;
	}

	private Message parsePosition(Element e) throws XMLException {

		// Create the bean
		PositionMessage msg = new PositionMessage(_user);
		msg.setTime(_timeStamp);

		// Get the basic information
		try {
			msg.setHeading(Integer.parseInt(getChildText(e, "heading", "0")));
			msg.setLatitude(Double.parseDouble(getChildText(e, "lat", "0")));
			msg.setLongitude(Double.parseDouble(getChildText(e, "lon", "0")));
			msg.setAltitude(Integer.parseInt(getChildText(e, "alt_msl", "0")));
			msg.setRadarAltitude(Integer.parseInt(getChildText(e, "alt_agl", "0")));
			msg.setAspeed(Integer.parseInt(getChildText(e, "air_speed", "0")));
			msg.setGspeed(Integer.parseInt(getChildText(e, "ground_speed", "0")));
			msg.setVspeed(Integer.parseInt(getChildText(e, "vert_speed", "0")));
			msg.setMach(Double.parseDouble(getChildText(e, "mach_num", "0")));
			msg.setFuelRemaining(Integer.parseInt(getChildText(e, "fuel", "0")));
			msg.setFlaps(Integer.parseInt(getChildText(e, "flaps", "0")));
			msg.setFlags(Integer.parseInt(getChildText(e, "flags", "0")));
			msg.setN1(Double.parseDouble(getChildText(e, "avg_n1", "0")));
			msg.setN2(Double.parseDouble(getChildText(e, "avg_n2", "0")));
			msg.setPhase(getChildText(e, "phase", PositionMessage.FLIGHT_PHASES[PositionMessage.PHASE_UNKNOWN]));
			msg.setSimRate(Integer.parseInt(getChildText(e, "simrate", "256")));
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

	private Message parseInfo(Element e) {

		// Create the bean
		InfoMessage msg = new InfoMessage(_user);
		msg.setTime(_timeStamp);

		// Load the bean
		msg.setEquipmentType(getChildText(e, "equipment", "UNKNOWN"));
		msg.setFlightCode(getChildText(e, "flight_num", "???"));
		msg.setAltitude(getChildText(e, "cruise_alt", null));
		msg.setWaypoints(getChildText(e, "route", "DIRECT"));
		msg.setComments(getChildText(e, "remarks", null));
		msg.setFSVersion(Integer.parseInt(getChildText(e, "fsversion", "7")));
		msg.setAirportD(SystemData.getAirport(getChildText(e, "dep_apt", null)));
		msg.setAirportA(SystemData.getAirport(getChildText(e, "arr_apt", null)));

		// Return the bean
		return msg;
	}

	private Message parseDiag() {

		// Create the bean
		DiagnosticMessage msg = new DiagnosticMessage(_user);
		msg.setTime(_timeStamp);

		// Set the request type
		msg.setRequestType(getChildText("reqtype", ""));
		msg.setRequestData(getChildText("reqdata", null));

		// Return the bean
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
	   
	   // Parse the PIREP data
	   Element pe = _el.getChild("pirep");
	   if (pe == null)
	      throw new XMLException("No PIREP data in Message");
	   
	   // Build the PIREP
	   ACARSFlightReport afr = ACARSHelper.create(getChildText(pe, "flightcode", "001"));
	   try {
	   	afr.setAttribute(FlightReport.ATTR_ACARS, true);
      	afr.setDatabaseID(FlightReport.DBID_ACARS, Integer.parseInt(pe.getChildTextTrim("flight_id")));
      	afr.setDatabaseID(FlightReport.DBID_PILOT, _user.getID());
			afr.setRank(_user.getRank());
      	afr.setStatus(FlightReport.SUBMITTED);
      	afr.setEquipmentType(pe.getChildTextTrim("equipment"));
      	afr.setDate(new Date());
      	afr.setCreatedOn(afr.getDate());
      	afr.setSubmittedOn(afr.getDate());
      	afr.setFSVersion("FS" + pe.getChildTextTrim("fsVersion"));
      	afr.setAirportD(SystemData.getAirport(pe.getChildTextTrim("dep_apt").toUpperCase()));
      	afr.setAirportA(SystemData.getAirport(pe.getChildTextTrim("arr_apt").toUpperCase()));
      	afr.setRemarks(pe.getChildText("remarks"));
      	
         // Get the online network
         String network = pe.getChildTextTrim("network").toUpperCase();
         if ("VATSIM".equals(network)) {
            afr.setAttribute(FlightReport.ATTR_VATSIM, true);
         } else if ("IVAO".equals(network)) {
            afr.setAttribute(FlightReport.ATTR_IVAO, true);
         } else if ("FPI".equals(network)) {
            afr.setAttribute(FlightReport.ATTR_FPI, true);
         }

         // Set the times
         try {
            afr.setStartTime(_dtf.parse(pe.getChildTextTrim("startTime")));
            afr.setEngineStartTime(_dtf.parse(pe.getChildTextTrim("engineStartTime")));
         	afr.setTaxiTime(_dtf.parse(pe.getChildTextTrim("taxiOutTime")));
         	afr.setTakeoffTime(_dtf.parse(pe.getChildTextTrim("takeoffTime")));
         	afr.setLandingTime(_dtf.parse(pe.getChildTextTrim("landingTime")));
         	afr.setEndTime(_dtf.parse(pe.getChildTextTrim("gateTime")));
         } catch (ParseException pex) {
            throw new XMLException("Invalid Date/Time - " + pex.getMessage(), pex);
         }
         
         // Calculate the flight time
         int duration = (int) ((afr.getEndTime().getTime() - afr.getStartTime().getTime()) / 1000);
         afr.setLength(duration / 36);
         
         // Set the weights/speeds
         try {
            afr.setTaxiFuel(Integer.parseInt(getChildText(pe, "fuel_at_taxi", "0")));
            afr.setTaxiWeight(Integer.parseInt(getChildText(pe, "weight_at_taxi", "0")));
            afr.setTakeoffFuel(Integer.parseInt(getChildText(pe, "fuel_at_takeoff", "0")));
            afr.setTakeoffWeight(Integer.parseInt(getChildText(pe, "weight_at_takeoff", "0")));
            afr.setTakeoffN1(Double.parseDouble(getChildText(pe, "n1_at_takeoff", "0")));
            afr.setTakeoffSpeed(Integer.parseInt(getChildText(pe, "takeoff_speed", "0")));
            afr.setLandingFuel(Integer.parseInt(getChildText(pe, "fuel_at_landing", "0")));
            afr.setLandingWeight(Integer.parseInt(getChildText(pe, "weight_at_landing", "0")));
            afr.setLandingN1(Double.parseDouble(getChildText(pe, "n1_at_landing", "0")));
            afr.setLandingSpeed(Integer.parseInt(getChildText(pe, "landing_speed", "0")));
            afr.setLandingVSpeed(Integer.parseInt(getChildText(pe, "landing_vspeed", "0")) * -1);
            afr.setGateFuel(Integer.parseInt(getChildText(pe, "fuel_at_gate", "0")));
            afr.setGateWeight(Integer.parseInt(getChildText(pe, "weight_at_gate", "0")));
         } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Invalid Weight/Speed - " + nfe.getMessage());
         }
	   } catch (Exception e) {
	      throw new XMLException("Invalid PIREP data - " + e.getMessage(), e);
	   }
	   
	   // Save the PIREP and mark if we are offline
	   msg.setPIREP(afr);
	   
	   // If this was an offline PIREP, then load the info
	   Element ie = _el.getChild("info");
	   if ((ie != null) && msg.isOffline())
	      msg.setInfo((InfoMessage) parseInfo(ie));
	   
	   // If this was an offline PIREP, then load the positions
	   Element pse = _el.getChild("positions");
	   if ((pse != null) && msg.isOffline()) {
	      List positions = _el.getChildren("position");
	      for (Iterator i = positions.iterator(); i.hasNext(); ) {
	         Element posE = (Element) i.next();
	         msg.addPosition((PositionMessage) parsePosition(posE));
	      }
	   }
	   
	   // Return the message
	   return msg;
	}
}