package org.deltava.acars.xml;

import java.util.Iterator;
import java.text.DecimalFormat;

import org.jdom.Element;

import org.deltava.beans.Pilot;
import org.deltava.beans.navdata.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

/**
 * V1 Protocol Message Formatter.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

class MessageFormatterV1 implements MessageFormatter {

	private static final int PROTOCOL_VERSION = 1;
	
	// Number formatters
	private static final DecimalFormat _mf = new DecimalFormat("0.00");
	private static final DecimalFormat _nxf = new DecimalFormat("#00.0");
	private static final DecimalFormat _hdgf = new DecimalFormat("000");
	private static final DecimalFormat _dgf = new DecimalFormat("##0.0000");

	MessageFormatterV1() {
		super();
	}

	public int getProtocolVersion() {
		return PROTOCOL_VERSION;
	}
	
	private Element createElement(String eName, String eValue) {
		Element e = new Element(eName);
		e.setText(eValue);
		return e;
	}

	public Element format(Message msgBean) throws XMLException {
		
		// Run a different function depending on the bean type
		switch (msgBean.getType()) {
			case Message.MSG_ACK :
				return formatAck((AcknowledgeMessage) msgBean);
			
			case Message.MSG_DATARSP :
				return formatDataRsp((DataResponseMessage) msgBean);
			
			case Message.MSG_DIAG :
				return formatDiag((DiagnosticMessage) msgBean);
			
			case Message.MSG_INFO :
				return formatInfo((InfoMessage) msgBean);
			
			case Message.MSG_POSITION :
				return formatPosition((PositionMessage) msgBean);
			
			case Message.MSG_SYSTEM :
				return formatSystem((SystemTextMessage) msgBean);
			
			case Message.MSG_TEXT :
				return formatText((TextMessage) msgBean);
				
			// if for some reason we get a raw message ignore it
			case Message.MSG_RAW :
				return null;
			
			default :
				throw new XMLException("Invalid message type");
		}
	}
	
	private Element formatAck(AcknowledgeMessage msg) throws XMLException {
		
		try {
			// Create the element and the type
			Element e = new Element(ProtocolInfo.CMD_ELEMENT_NAME);
			e.setAttribute("type", Message.MSG_CODES[msg.getType()]);
			e.setAttribute("id", Long.toHexString(msg.getParentID()).toUpperCase());
			
			// Display additional elements
			Iterator i = msg.getEntryNames();
			while (i.hasNext()) {
				String eName = (String) i.next();
				e.addContent(createElement(eName, msg.getEntry(eName)));
			}
			
			// Return the element
			return e;
		} catch (Exception e) {
			throw new XMLException("Error formatting acknowledge message - " + e.getMessage(), e);
		}
	}
	
	private Element formatSystem(SystemTextMessage msg) throws XMLException {
		try {
			Element e = new Element(ProtocolInfo.CMD_ELEMENT_NAME);
			e.setAttribute("type", Message.MSG_CODES[msg.getType()]);
			e.setAttribute("msgtype", "text");
			e.addContent(createElement("time", Long.toHexString(msg.getTime())));
			for (Iterator i = msg.getMsgs().iterator(); i.hasNext(); ) {
				String msgText = (String) i.next();
				e.addContent(createElement("text", msgText));
			}
			
			return e;
		} catch (Exception e) {
			throw new XMLException("Error formatting text message - " + e.getMessage(), e);
		}
	}
	
	private Element formatText(TextMessage msg) throws XMLException {
		try {
			// Create the element and the type
			Element e = new Element(ProtocolInfo.CMD_ELEMENT_NAME);
			e.setAttribute("type", Message.MSG_CODES[msg.getType()]);
		
			// Set information about the message
			e.addContent(createElement("from", msg.getSenderID()));
			e.addContent(createElement("text", msg.getText()));
			e.addContent(createElement("time", Long.toHexString(msg.getTime())));
			if (!msg.isPublic())
				e.addContent(createElement("to", msg.getRecipient()));

			return e;
		} catch (Exception e) {
			throw new XMLException("Error formatting text message - " + e.getMessage(), e);
		}
	}
	
	private Element formatPosition(PositionMessage msg) throws XMLException {
		
		try {
			// Create the element and the type
			Element e = new Element(ProtocolInfo.CMD_ELEMENT_NAME);
			e.setAttribute("type", Message.MSG_CODES[msg.getType()]);
			
			// Set element information
			e.addContent(createElement("from", msg.getSenderID()));
			synchronized (_dgf) {
				e.addContent(createElement("lat", _dgf.format(msg.getLatitude())));
				e.addContent(createElement("lon", _dgf.format(msg.getLongitude())));
			}
			e.addContent(createElement("heading", _hdgf.format(msg.getHeading())));
			e.addContent(createElement("alt_msl", String.valueOf(msg.getAltitude())));
			e.addContent(createElement("alt_agl", String.valueOf(msg.getRadarAltitude())));
			e.addContent(createElement("mach_num", _mf.format(msg.getMach())));
			e.addContent(createElement("air_speed", String.valueOf(msg.getAspeed())));
			e.addContent(createElement("ground_speed", String.valueOf(msg.getGspeed())));
			e.addContent(createElement("vert_speed", String.valueOf(msg.getVspeed())));
			e.addContent(createElement("fuel", String.valueOf(msg.getFuelRemaining())));
			e.addContent(createElement("flaps", String.valueOf(msg.getFlaps())));
			e.addContent(createElement("time", Long.toHexString(msg.getTime())));
			synchronized (_nxf) {
				e.addContent(createElement("avg_n1", _nxf.format(msg.getN1())));
				e.addContent(createElement("avg_n2", _nxf.format(msg.getN2())));
			}
			
			// Create optional elements
			if (msg.isFlagSet(PositionMessage.FLAG_AFTERBURNER)) e.addContent(createElement("afterburner", "1"));
			if (msg.isFlagSet(PositionMessage.FLAG_PAUSED)) e.addContent(createElement("paused", "1"));
			if (msg.isFlagSet(PositionMessage.FLAG_SLEW)) e.addContent(createElement("slew", "1"));
			if (msg.getSimRate() != 1) e.addContent(createElement("simrate", String.valueOf(msg.getSimRate())));
			
			// Return the element
			return e;
		} catch (Exception e) {
			throw new XMLException("Error formatting position message - " + e.getMessage(), e);
		}
	}

	private Element formatInfo(InfoMessage msg) throws XMLException {

		try {
			// Create the element and the type
			Element e = new Element(ProtocolInfo.CMD_ELEMENT_NAME);
			e.setAttribute("type", Message.MSG_CODES[msg.getType()]);

			// Set element information
			e.addContent(createElement("from", msg.getSenderID()));
			e.addContent(createElement("equipment", msg.getEquipmentType()));
			e.addContent(createElement("flight_num", msg.getFlightCode()));
			e.addContent(createElement("dep_apt", msg.getAirportD().getICAO()));
			e.addContent(createElement("arr_apt", msg.getAirportA().getICAO()));
			e.addContent(createElement("alt_apt", msg.getAirportL().getICAO()));
			e.addContent(createElement("cruise_alt", msg.getAltitude()));
			e.addContent(createElement("route", msg.getAllWaypoints()));
			e.addContent(createElement("remarks", msg.getComments()));
			e.addContent(createElement("time", Long.toHexString(msg.getTime())));			
			
			// Return the element
			return e;
		} catch (Exception e) {
			throw new XMLException("Error formatting info message - " + e.getMessage(), e);
		}
	}
	
	private Element formatDiag(DiagnosticMessage msg) throws XMLException {
		try {
			// Create the element and the type
			Element e = new Element(ProtocolInfo.CMD_ELEMENT_NAME);
			e.setAttribute("type", Message.MSG_CODES[msg.getType()]);
			
			// Save the type
			e.addContent(createElement("reqtype", DiagnosticMessage.MSG_TYPES[msg.getRequestType()]));
			e.addContent(createElement("reqData", msg.getRequestData()));
			e.addContent(createElement("time", Long.toHexString(msg.getTime())));			
			
			// Return the element
			return e;
		} catch (Exception e) {
			throw new XMLException("Error formatting diagnostic message - " + e.getMessage(), e);
		}
	}
	
	private Element formatNavaid(NavigationRadioBean nav) throws XMLException {
		try {
			// Create the element
			Element e = new Element("navaid");
			
			// Get the navaid info
			NavigationDataBean navaid = nav.getNavaid();
			
			// Add navaid info
			e.addContent(createElement("radio", nav.getRadio()));
			e.addContent(createElement("type", navaid.getTypeName()));
			e.addContent(createElement("code", navaid.getCode()));
			if (navaid instanceof VOR) {
				e.addContent(createElement("freq", ((VOR) navaid).getFrequency()));
				e.addContent(createElement("hdg", nav.getHeading()));
			} else if (navaid instanceof Runway) {
				Runway rwy = (Runway) navaid;
				e.addContent(createElement("freq", rwy.getFrequency()));
				e.addContent(createElement("hdg", String.valueOf(rwy.getHeading())));
			}
				
			// Return the element
			return e;
		} catch (Exception e) {
			throw new XMLException("Error formatting navaid info message - " + e.getMessage(), e);
		}
	}
	
	private Element formatConnection(ACARSConnection con) throws XMLException {
		try {
			// Create the element
			Element e = new Element("Pilot");

			// Display user-specific stuff
			if (con.isAuthenticated()) {
				Pilot userInfo = con.getUser();
				e.setAttribute("id", userInfo.getPilotCode());
				e.addContent(createElement("firstname", userInfo.getFirstName()));
				e.addContent(createElement("lastname", userInfo.getLastName()));
				e.addContent(createElement("name", userInfo.getName()));
				e.addContent(createElement("eqtype", userInfo.getEquipmentType()));
				e.addContent(createElement("rank", userInfo.getRank()));
			}
			
			// Add connection specific stuff
			e.addContent(createElement("protocol", String.valueOf(con.getProtocolVersion())));
			e.addContent(createElement("remoteaddr", con.getRemoteAddr()));
			e.addContent(createElement("remotehost", con.getRemoteHost()));
			e.addContent(createElement("starttime", Long.toHexString(con.getStartTime())));
			e.addContent(createElement("lastactivity", con.getFormatLastActivityTime()));
			e.addContent(createElement("input", String.valueOf(con.getBytesIn())));
			e.addContent(createElement("output", String.valueOf(con.getBytesOut())));
			e.addContent(createElement("msginput", String.valueOf(con.getMsgsIn())));
			e.addContent(createElement("msgoutput", String.valueOf(con.getMsgsOut())));
			
			// Return the element
			return e;
		} catch (Exception e) {
			throw new XMLException("Error formatting user info message - " + e.getMessage(), e);
		}
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
					case Message.MSG_INFO :
						childE = formatInfo((InfoMessage) rspMsg);
						break;
						
					case Message.MSG_POSITION :
						childE = formatPosition((PositionMessage) rspMsg);
						break;														 
						
					default :
						// do nothing
					}
					
					// Change the element type and add the child element to this one
					childE.setName(childE.getAttributeValue("type"));
					childE.removeAttribute("type");
					e.addContent(childE);
				} else if (rsp instanceof ACARSConnection) {
					e.addContent(createElement("rsptype", "pilotlist"));
					e.addContent(formatConnection((ACARSConnection) rsp));
				} else if (rsp instanceof NavigationRadioBean) {
					e.addContent(createElement("rsptype", "navaid"));
					e.addContent(formatNavaid((NavigationRadioBean) rsp));
				} else if (rsp instanceof DataResponseMessage.TextElement) {
				   DataResponseMessage.TextElement txt = (DataResponseMessage.TextElement) rsp;
				   e.addContent(createElement("rsptype", "info"));
				   e.addContent(createElement(txt.getName(), txt.getValue()));
				}
			}

			// return the element			
			return e;
		} catch (Exception e) {
			throw new XMLException("Error formatting data request message - " + e.getMessage(), e);
		}
	}
}