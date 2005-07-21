// Copyright 2004, 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.xml;

import java.util.*;

import org.jdom.Element;

import org.deltava.beans.Pilot;
import org.deltava.acars.message.*;

import org.deltava.util.system.SystemData;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

class MessageParserV1 implements MessageParser {

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
		String tmp = _el.getChildTextTrim(childName);
		return (tmp == null) ? defaultValue : tmp;
	}

	public Message parse(int msgType) throws XMLException {

		// Depending on the message type, call a parse method
		switch (msgType) {
			case Message.MSG_POSITION:
				return parsePosition();

			case Message.MSG_INFO:
				return parseInfo();

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

	private Message parsePosition() throws XMLException {

		// Create the bean
		PositionMessage msg = new PositionMessage(_user);
		msg.setTime(_timeStamp);

		// Get the basic information
		try {
			msg.setHeading(Integer.parseInt(getChildText("heading", "0")));
			msg.setLatitude(Double.parseDouble(getChildText("lat", "0")));
			msg.setLongitude(Double.parseDouble(getChildText("lon", "0")));
			msg.setAltitude(Integer.parseInt(getChildText("alt_msl", "0")));
			msg.setRadarAltitude(Integer.parseInt(getChildText("alt_agl", "0")));
			msg.setAspeed(Integer.parseInt(getChildText("air_speed", "0")));
			msg.setGspeed(Integer.parseInt(getChildText("ground_speed", "0")));
			msg.setVspeed(Integer.parseInt(getChildText("vert_speed", "0")));
			msg.setMach(Double.parseDouble(getChildText("mach_num", "0")));
			msg.setFuelRemaining(Integer.parseInt(getChildText("fuel", "0")));
			msg.setFlaps(Integer.parseInt(getChildText("flaps", "0")));
			msg.setFlags(Integer.parseInt(getChildText("flags", "0")));
			msg.setN1(Double.parseDouble(getChildText("avg_n1", "0")));
			msg.setN2(Double.parseDouble(getChildText("avg_n2", "0")));
			msg.setPhase(getChildText("phase", PositionMessage.FLIGHT_PHASES[PositionMessage.PHASE_UNKNOWN]));
			msg.setSimRate(Integer.parseInt(getChildText("simrate", "256")));
		} catch (Exception e) {
			throw new XMLException("Error parsing Position data - " + e.getMessage(), e);
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

	private Message parseInfo() {

		// Create the bean
		InfoMessage msg = new InfoMessage(_user);
		msg.setTime(_timeStamp);

		// Load the bean
		msg.setEquipmentType(getChildText("equipment", "UNKNOWN"));
		msg.setFlightCode(getChildText("flight_num", "???"));
		msg.setAltitude(getChildText("cruise_alt", null));
		msg.setWaypoints(getChildText("route", "DIRECT"));
		msg.setComments(getChildText("remarks", null));
		msg.setFSVersion(Integer.parseInt(getChildText("fsversion", "7")));
		msg.setAirportD(SystemData.getAirport(getChildText("dep_apt", null)));
		msg.setAirportA(SystemData.getAirport(getChildText("arr_apt", null)));
		msg.setAirportL(SystemData.getAirport(getChildText("alt_apt", null)));

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
}