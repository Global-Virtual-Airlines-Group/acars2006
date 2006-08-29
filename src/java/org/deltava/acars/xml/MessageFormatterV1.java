// Copyright 2004, 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml;

import java.util.*;
import java.text.DecimalFormat;

import org.apache.log4j.Logger;

import org.jdom.Element;

import org.deltava.beans.*;
import org.deltava.beans.acars.ACARSFlags;
import org.deltava.beans.navdata.*;
import org.deltava.beans.servinfo.Controller;
import org.deltava.beans.schedule.*;
import org.deltava.beans.ts2.Server;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import org.deltava.util.*;

/**
 * V1 Protocol Message Formatter.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

class MessageFormatterV1 implements MessageFormatter {

	private static final Logger log = Logger.getLogger(MessageFormatter.class);

	private static final int PROTOCOL_VERSION = 1;

	// Number formatters
	private final DecimalFormat _mf = new DecimalFormat("0.00");
	private final DecimalFormat _nxf = new DecimalFormat("#00.0");
	private final DecimalFormat _hdgf = new DecimalFormat("000");
	private final DecimalFormat _dgf = new DecimalFormat("##0.0000");

	MessageFormatterV1() {
		super();
	}

	public int getProtocolVersion() {
		return PROTOCOL_VERSION;
	}

	public Element format(Message msgBean) throws XMLException {

		// Run a different function depending on the bean type
		try {
			switch (msgBean.getType()) {
				case Message.MSG_ACK:
					return formatAck((AcknowledgeMessage) msgBean);

				case Message.MSG_DATARSP:
					return formatDataRsp((DataResponseMessage) msgBean);

				case Message.MSG_DIAG:
					return formatDiag((DiagnosticMessage) msgBean);

				case Message.MSG_DISPATCH:
					return formatDispatch((DispatchMessage) msgBean);

				case Message.MSG_INFO:
					return formatInfo((InfoMessage) msgBean);

				case Message.MSG_POSITION:
					return formatPosition((PositionMessage) msgBean);

				case Message.MSG_SYSTEM:
					return formatSystem((SystemTextMessage) msgBean);

				case Message.MSG_TEXT:
					return formatText((TextMessage) msgBean);

				// if for some reason we get a raw message ignore it
				case Message.MSG_RAW:
				case Message.MSG_QUIT:
					return null;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new XMLException("Error formatting " + msgBean.getType() + " message - " + e.getMessage(), e);
		}

		throw new XMLException("Invalid message type - " + msgBean.getType());
	}

	private Element formatAck(AcknowledgeMessage msg) throws XMLException {

		// Create the element and the type
		Element e = new Element(ProtocolInfo.CMD_ELEMENT_NAME);
		e.setAttribute("type", Message.MSG_CODES[msg.getType()]);
		e.setAttribute("id", Long.toHexString(msg.getParentID()).toUpperCase());

		// Display additional elements
		for (Iterator<String> i = msg.getEntryNames().iterator(); i.hasNext();) {
			String eName = i.next();
			e.addContent(XMLUtils.createElement(eName, msg.getEntry(eName), true));
		}

		// Return the element
		return e;
	}

	private Element formatDispatch(DispatchMessage msg) throws XMLException {
		
		// Create the element
		Element e = new Element(ProtocolInfo.CMD_ELEMENT_NAME);
		e.setAttribute("type", Message.MSG_CODES[msg.getType()]);
		
		// Write the elements
		e.addContent(XMLUtils.createElement("flightCode", msg.getFlightCode()));
		e.addContent(XMLUtils.createElement("leg", String.valueOf(msg.getLeg())));
		e.addContent(XMLUtils.createElement("eqType", msg.getEquipmentType()));
		e.addContent(formatAirport(msg.getAirportD(), "airportD"));
		e.addContent(formatAirport(msg.getAirportA(), "airportA"));
		e.addContent(formatAirport(msg.getAirportL(), "airportL"));
		e.addContent(XMLUtils.createElement("route", msg.getRoute()));
		e.addContent(XMLUtils.createElement("fuel", String.valueOf(msg.getFuel())));
		e.addContent(XMLUtils.createElement("fuel", StringUtils.format(msg.getTXCode(), "0000")));
		return e;
	}

	private Element formatSystem(SystemTextMessage msg) throws XMLException {

		Element e = new Element(ProtocolInfo.CMD_ELEMENT_NAME);
		e.setAttribute("type", Message.MSG_CODES[msg.getType()]);
		e.setAttribute("msgtype", "text");
		e.addContent(XMLUtils.createElement("time", Long.toHexString(msg.getTime())));
		for (Iterator<String> i = msg.getMsgs().iterator(); i.hasNext();) {
			String msgText = i.next();
			e.addContent(XMLUtils.createElement("text", msgText, true));
		}

		return e;
	}

	private Element formatText(TextMessage msg) throws XMLException {

		// Create the element and the type
		Element e = new Element(ProtocolInfo.CMD_ELEMENT_NAME);
		e.setAttribute("type", Message.MSG_CODES[msg.getType()]);

		// Set information about the message
		e.addContent(XMLUtils.createElement("from", msg.getSenderID()));
		e.addContent(XMLUtils.createElement("text", msg.getText()));
		e.addContent(XMLUtils.createElement("time", Long.toHexString(msg.getTime())));
		if (!msg.isPublic())
			e.addContent(XMLUtils.createElement("to", msg.getRecipient()));

		return e;
	}

	private Element formatAirport(Airport a, String eName) {
		Element ae = new Element(eName);
		if (a != null) {
			ae.setAttribute("name", a.getName());
			ae.setAttribute("icao", a.getICAO());
			ae.setAttribute("iata", a.getIATA());
			ae.setAttribute("lat", StringUtils.format(a.getLatitude(), "##0.0000"));
			ae.setAttribute("lng", StringUtils.format(a.getLongitude(), "##0.0000"));
		}

		return ae;
	}

	private Element formatPosition(PositionMessage msg) throws XMLException {

		// Create the element and the type
		Element e = new Element(ProtocolInfo.CMD_ELEMENT_NAME);
		e.setAttribute("type", Message.MSG_CODES[msg.getType()]);

		// Set element information
		e.addContent(XMLUtils.createElement("from", msg.getSenderID()));
		synchronized (_dgf) {
			e.addContent(XMLUtils.createElement("lat", _dgf.format(msg.getLatitude())));
			e.addContent(XMLUtils.createElement("lon", _dgf.format(msg.getLongitude())));
		}
		e.addContent(XMLUtils.createElement("hdg", _hdgf.format(msg.getHeading())));
		e.addContent(XMLUtils.createElement("msl", String.valueOf(msg.getAltitude())));
		e.addContent(XMLUtils.createElement("agl", String.valueOf(msg.getRadarAltitude())));
		e.addContent(XMLUtils.createElement("mach", _mf.format(msg.getMach())));
		e.addContent(XMLUtils.createElement("air_speed", String.valueOf(msg.getAspeed())));
		e.addContent(XMLUtils.createElement("ground_speed", String.valueOf(msg.getGspeed())));
		e.addContent(XMLUtils.createElement("vert_speed", String.valueOf(msg.getVspeed())));
		e.addContent(XMLUtils.createElement("fuel", String.valueOf(msg.getFuelRemaining())));
		e.addContent(XMLUtils.createElement("flaps", String.valueOf(msg.getFlaps())));
		e.addContent(XMLUtils.createElement("time", Long.toHexString(msg.getTime())));
		synchronized (_nxf) {
			e.addContent(XMLUtils.createElement("avg_n1", _nxf.format(msg.getN1())));
			e.addContent(XMLUtils.createElement("avg_n2", _nxf.format(msg.getN2())));
		}

		// Create optional elements
		if (msg.isFlagSet(ACARSFlags.FLAG_AFTERBURNER))
			e.addContent(XMLUtils.createElement("afterburner", "1"));
		if (msg.isFlagSet(ACARSFlags.FLAG_PAUSED))
			e.addContent(XMLUtils.createElement("paused", "1"));
		if (msg.getSimRate() != 1)
			e.addContent(XMLUtils.createElement("simrate", String.valueOf(msg.getSimRate())));

		// Return the element
		return e;
	}

	private Element formatInfo(InfoMessage msg) throws XMLException {

		// Create the element and the type
		Element e = new Element(ProtocolInfo.CMD_ELEMENT_NAME);
		e.setAttribute("type", Message.MSG_CODES[msg.getType()]);

		// Set element information
		e.addContent(XMLUtils.createElement("id", msg.getSenderID()));
		e.addContent(XMLUtils.createElement("name", msg.getSender().getName()));
		e.addContent(XMLUtils.createElement("equipment", msg.getEquipmentType()));
		e.addContent(XMLUtils.createElement("flight_num", msg.getFlightCode()));
		e.addContent(XMLUtils.createElement("dep_apt", msg.getAirportD().getICAO()));
		e.addContent(XMLUtils.createElement("arr_apt", msg.getAirportA().getICAO()));
		e.addContent(XMLUtils.createElement("cruise_alt", msg.getAltitude()));
		e.addContent(XMLUtils.createElement("route", msg.getAllWaypoints()));
		e.addContent(XMLUtils.createElement("remarks", msg.getComments()));
		e.addContent(XMLUtils.createElement("time", Long.toHexString(msg.getTime())));

		// Return the element
		return e;
	}

	private Element formatDiag(DiagnosticMessage msg) throws XMLException {

		// Create the element and the type
		Element e = new Element(ProtocolInfo.CMD_ELEMENT_NAME);
		e.setAttribute("type", Message.MSG_CODES[msg.getType()]);

		// Save the type
		e.addContent(XMLUtils.createElement("reqtype", Message.MSG_TYPES[msg.getRequestType()]));
		e.addContent(XMLUtils.createElement("reqData", msg.getRequestData()));
		e.addContent(XMLUtils.createElement("time", Long.toHexString(msg.getTime())));

		// Return the element
		return e;
	}

	private Element formatNavaid(NavigationRadioBean nav) throws XMLException {

		// Create the element
		Element e = new Element("navaid");

		// Get the navaid info
		NavigationDataBean navaid = nav.getNavaid();

		// Add navaid info
		e.addContent(XMLUtils.createElement("radio", nav.getRadio()));
		e.addContent(XMLUtils.createElement("type", navaid.getTypeName()));
		e.addContent(XMLUtils.createElement("code", navaid.getCode()));
		if (navaid instanceof VOR) {
			e.addContent(XMLUtils.createElement("freq", ((VOR) navaid).getFrequency()));
			e.addContent(XMLUtils.createElement("hdg", nav.getHeading()));
		} else if (navaid instanceof NDB) {
			e.addContent(XMLUtils.createElement("freq", ((NDB) navaid).getFrequency()));
		}

		// Return the element
		return e;
	}

	private Element formatConnection(ACARSConnection con) throws XMLException {

		// Create the element
		Element e = new Element("Pilot");

		// Display user-specific stuff
		if (con.isAuthenticated()) {
			Pilot usr = con.getUser();
			e.setAttribute("id", usr.getPilotCode());
			e.addContent(XMLUtils.createElement("firstname", usr.getFirstName()));
			e.addContent(XMLUtils.createElement("lastname", usr.getLastName()));
			e.addContent(XMLUtils.createElement("name", usr.getName()));
			e.addContent(XMLUtils.createElement("eqtype", usr.getEquipmentType()));
			e.addContent(XMLUtils.createElement("rank", usr.getRank()));
			e.addContent(XMLUtils.createElement("hours", String.valueOf(usr.getHours())));
			e.addContent(XMLUtils.createElement("legs", String.valueOf(usr.getLegs())));
			e.addContent(XMLUtils.createElement("joinedOn", StringUtils.format(usr.getCreatedOn(), "MMMM dd, yyyy")));
			e.addContent(XMLUtils.createElement("isBusy", String.valueOf(con.getUserBusy())));
			e.addContent(XMLUtils.createElement("isDispatch", String.valueOf(con.getIsDispatch())));
			e.addContent(XMLUtils.createElement("isHidden", String.valueOf(con.getUserHidden())));
			e.addContent(XMLUtils.createElement("roles", StringUtils.listConcat(usr.getRoles(), ",")));
		}

		// Display flight-specific stuff
		InfoMessage inf = con.getFlightInfo();
		if (inf != null) {
			e.addContent(XMLUtils.createElement("flightCode", inf.getFlightCode()));
			e.addContent(XMLUtils.createElement("flightEQ", inf.getEquipmentType()));
			e.addContent(XMLUtils.createElement("airportD", inf.getAirportD().getICAO()));
			e.addContent(XMLUtils.createElement("airportA", inf.getAirportA().getICAO()));
		}

		// Add connection specific stuff
		e.addContent(XMLUtils.createElement("protocol", String.valueOf(con.getProtocolVersion())));
		e.addContent(XMLUtils.createElement("clientBuild", String.valueOf(con.getClientVersion())));
		e.addContent(XMLUtils.createElement("remoteaddr", con.getRemoteAddr()));
		e.addContent(XMLUtils.createElement("remotehost", con.getRemoteHost()));
		e.addContent(XMLUtils.createElement("starttime", Long.toHexString(con.getStartTime())));
		e.addContent(XMLUtils.createElement("lastactivity", StringUtils.formatHex(con.getLastActivity())));
		e.addContent(XMLUtils.createElement("input", String.valueOf(con.getBytesIn())));
		e.addContent(XMLUtils.createElement("output", String.valueOf(con.getBytesOut())));
		e.addContent(XMLUtils.createElement("msginput", String.valueOf(con.getMsgsIn())));
		e.addContent(XMLUtils.createElement("msgoutput", String.valueOf(con.getMsgsOut())));

		// Return the element
		return e;
	}

	private Element getData(Element cmd, String rspType) {
		// Get the element
		Element e = cmd.getChild(rspType);
		if (e != null)
			return e;

		// Create the new element
		cmd.addContent(XMLUtils.createElement("rsptype", rspType));
		e = new Element(rspType);
		cmd.addContent(e);
		return e;
	}

	private Element formatDataRsp(DataResponseMessage msg) throws XMLException {

		try {
			// Create the element and the type
			Element e = new Element(ProtocolInfo.CMD_ELEMENT_NAME);
			e.setAttribute("type", Message.MSG_CODES[msg.getType()]);

			// Loop through the response and format them using existing formatters
			Iterator i = msg.getResponse().iterator();
			while (i.hasNext()) {
				Object rsp = i.next();
				if (rsp instanceof Message) {
					Message rspMsg = (Message) rsp;
					Element childE = null;
					switch (rspMsg.getType()) {
						case Message.MSG_INFO:
							childE = formatInfo((InfoMessage) rspMsg);
							break;

						case Message.MSG_POSITION:
							childE = formatPosition((PositionMessage) rspMsg);
							break;

						default:
					// do nothing
					}

					// Change the element type and add the child element to this one
					childE.setName(childE.getAttributeValue("type"));
					childE.removeAttribute("type");
					e.addContent(childE);
				} else if (rsp instanceof Pilot) {
					Pilot userInfo = (Pilot) rsp;
					Element pe = new Element("Pilot");
					pe.setAttribute("id", userInfo.getPilotCode());
					pe.addContent(XMLUtils.createElement("firstname", userInfo.getFirstName()));
					pe.addContent(XMLUtils.createElement("lastname", userInfo.getLastName()));
					pe.addContent(XMLUtils.createElement("name", userInfo.getName()));
					pe.addContent(XMLUtils.createElement("eqtype", userInfo.getEquipmentType()));
					pe.addContent(XMLUtils.createElement("rank", userInfo.getRank()));
					e.addContent(XMLUtils.createElement("hours", String.valueOf(userInfo.getHours())));
					e.addContent(XMLUtils.createElement("legs", String.valueOf(userInfo.getLegs())));
					e.addContent(XMLUtils.createElement("joinedOn", StringUtils.format(userInfo.getCreatedOn(),
							"MMMM dd, yyyy")));
					e.addContent(XMLUtils.createElement("roles", StringUtils.listConcat(userInfo.getRoles(), ",")));
					Element dpe = null;
					switch (msg.getRequestType()) {
						case DataMessage.REQ_ADDUSER:
							dpe = getData(e, "addpilots");
							break;

						case DataMessage.REQ_REMOVEUSER:
							dpe = getData(e, "delpilots");
							break;
					}

					if (dpe != null)
						dpe.addContent(pe);
				} else if (rsp instanceof ACARSConnection) {
					Element pList = null;
					switch (msg.getRequestType()) {
						case DataMessage.REQ_ADDUSER:
						case DataMessage.REQ_BUSY:
							pList = getData(e, "addpilots");
							break;

						case DataMessage.REQ_USRLIST:
							pList = getData(e, "pilotlist");
							break;
					}

					if (pList != null)
						pList.addContent(formatConnection((ACARSConnection) rsp));
				} else if (rsp instanceof Chart) {
					Chart c = (Chart) rsp;
					Airport a = c.getAirport();

					// Save the airport info
					Element cList = getData(e, "charts");
					cList.setAttribute("name", a.getName());
					cList.setAttribute("iata", a.getIATA());
					cList.setAttribute("icao", a.getICAO());

					// Add the cheart element
					Element ce = new Element("chart");
					ce.setAttribute("name", c.getName());
					ce.setAttribute("id", String.valueOf(c.getID()));
					cList.addContent(ce);
				} else if (rsp instanceof FlightReport) {
					FlightReport fr = (FlightReport) rsp;
					Element fList = getData(e, "pireps");
					Element pe = new Element("pirep");
					pe.setAttribute("id", StringUtils.formatHex(fr.getID()));
					pe.setAttribute("airline", fr.getAirline().getCode());
					pe.setAttribute("number", StringUtils.format(fr.getFlightNumber(), "#000"));
					pe.setAttribute("leg", String.valueOf(fr.getLeg()));
					pe.addContent(XMLUtils.createElement("eqType", fr.getEquipmentType()));
					pe.addContent(XMLUtils.createElement("airportA", fr.getAirportA().getICAO()));
					pe.addContent(XMLUtils.createElement("airportD", fr.getAirportD().getICAO()));
					pe.addContent(XMLUtils.createElement("remarks", fr.getRemarks(), true));
					if (fr.hasAttribute(FlightReport.ATTR_VATSIM))
						pe.setAttribute("network", OnlineNetwork.VATSIM);
					else if (fr.hasAttribute(FlightReport.ATTR_IVAO))
						pe.setAttribute("network", OnlineNetwork.IVAO);
					else if (fr.hasAttribute(FlightReport.ATTR_FPI))
						pe.setAttribute("network", OnlineNetwork.FPI);

					fList.addContent(pe);
				} else if (rsp instanceof Controller) {
					Controller ctr = (Controller) rsp;
					Element ctrList = getData(e, "atc");
					Element ce = new Element("ctr");
					ce.setAttribute("code", ctr.getCallsign());
					ce.setAttribute("name", ctr.getName());
					ce.setAttribute("rating", ctr.getRatingName());
					ce.setAttribute("type", ctr.getFacilityType());
					ce.setAttribute("freq", ctr.getFrequency());
					ce.setAttribute("networkID", String.valueOf(ctr.getID()));
					ctrList.addContent(ce);
				} else if (rsp instanceof Airport) {
					Airport a = (Airport) rsp;
					Element aList = getData(e, "airports");
					aList.addContent(formatAirport(a, "airport"));
				} else if (rsp instanceof Airline) {
					Airline al = (Airline) rsp;
					Element aList = getData(e, "airlines");
					Element ae = new Element("airline");
					ae.setAttribute("code", al.getCode());
					ae.setAttribute("name", al.getName());
					aList.addContent(ae);
				} else if (rsp instanceof Runway) {
					Runway r = (Runway) rsp;
					Element rwyE = getData(e, "runways");
					Element re = new Element("runway");
					re.setAttribute("icao", r.getCode());
					re.setAttribute("name", r.getName());
					re.setAttribute("hdg", String.valueOf(r.getHeading()));
					re.setAttribute("length", String.valueOf(r.getLength()));
					if ((r.getFrequency() != null) && (!"-".equals(r.getFrequency())))
						re.addContent(XMLUtils.createElement("freq", r.getFrequency()));

					rwyE.addContent(re);
				} else if (rsp instanceof NavigationRadioBean) {
					Element navE = getData(e, "navaid");
					navE.addContent(formatNavaid((NavigationRadioBean) rsp));
				} else if (rsp instanceof Server) {
					Server srv = (Server) rsp;
					Element srvsE = getData(e, "ts2servers");
					Element srvE = new Element("ts2server");
					srvE.setAttribute("port", String.valueOf(srv.getPort()));
					srvE.setAttribute("name", srv.getName());
					srvE.addContent(XMLUtils.createElement("desc", srv.getDescription()));
					srvsE.addContent(srvE);
				} else if (rsp instanceof DataResponseMessage.DataElement) {
					DataResponseMessage.DataElement de = (DataResponseMessage.DataElement) rsp;
					Element iE = getData(e, "info");
					Object eValue = de.getValue();
					if (eValue instanceof String) {
						iE.addContent(XMLUtils.createElement(de.getName(), (String) de.getValue()));
					} else if (eValue instanceof Collection) {
						Collection eValues = (Collection) eValue;
						for (Iterator vi = eValues.iterator(); vi.hasNext();) {
							String entryData = (String) vi.next();
							iE.addContent(XMLUtils.createElement(de.getName(), entryData));
						}
					}
				}
			}

			// return the element
			return e;
		} catch (Exception e) {
			throw new XMLException("Error formatting data request message - " + e.getMessage(), e);
		}
	}
}