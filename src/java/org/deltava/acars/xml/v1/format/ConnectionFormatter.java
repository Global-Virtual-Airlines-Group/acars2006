// Copyright 2004, 2005, 2006, 2008, 2009, 2010, 2011, 2012, 2018, 2019, 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import org.jdom2.Element;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.ConnectionMessage;

import org.deltava.beans.Pilot;
import org.deltava.beans.acars.ConnectionStats;

import org.deltava.util.*;

/**
 * An XML Formatter for ACARS Connection data messages.
 * @author Luke
 * @version 9.0
 * @since 1.0
 */

class ConnectionFormatter extends ElementFormatter {

	/**
	 * Formats a ConnectionMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {

		// Cast the message
		ConnectionMessage cmsg = (ConnectionMessage) msg;
		Pilot owner = cmsg.getSender();
		boolean isAdmin = (owner != null) && owner.isInRole("HR");
		long now = System.currentTimeMillis();

		// Create the parent elements
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, (cmsg.getRequestType() == DataRequest.USERLIST ) ? "pilotlist" : "addpilots");
		for (ACARSConnection con : cmsg.getResponse()) {
			Element ce = new Element("Pilot");
			ce.setAttribute("isVoice", String.valueOf(con.isVoiceEnabled()));
			if (con.isAuthenticated()) {
				Pilot usr = con.getUser();
				ce.setAttribute("id", usr.getPilotCode());
				ce.setAttribute("dbID", Integer.toHexString(usr.getID()));
				ce.setAttribute("appCode", con.getUserData().getAirlineCode());
				ce.addContent(XMLUtils.createElement("firstname", usr.getFirstName()));
				ce.addContent(XMLUtils.createElement("lastname", usr.getLastName()));
				ce.addContent(XMLUtils.createElement("name", usr.getName()));
				ce.addContent(XMLUtils.createElement("eqtype", usr.getEquipmentType()));
				ce.addContent(XMLUtils.createElement("rank", usr.getRank().getName()));
				ce.addContent(XMLUtils.createElement("hours", String.valueOf(usr.getHours())));
				ce.addContent(XMLUtils.createElement("legs", String.valueOf(usr.getLegs())));
				ce.addContent(XMLUtils.createElement("joinedOn", StringUtils.format(usr.getCreatedOn(), "MMMM dd, yyyy")));
				ce.addContent(XMLUtils.createElement("isBusy", String.valueOf(con.getUserBusy())));
				ce.addContent(XMLUtils.createElement("isDispatch", String.valueOf(con.getIsDispatch())));
				ce.addContent(XMLUtils.createElement("isVoice", String.valueOf(con.isVoiceEnabled() || con.getMuted())));
				ce.addContent(XMLUtils.createElement("isATC", String.valueOf(con.getIsATC())));
				ce.addContent(XMLUtils.createElement("isHidden", String.valueOf(con.getUserHidden())));
				ce.addContent(XMLUtils.createElement("roles", StringUtils.listConcat(usr.getRoles(), ",")));
				ce.addContent(XMLUtils.createElement("ratings", StringUtils.listConcat(usr.getRatings(), ",")));
				ce.addContent(XMLUtils.createElement("conTime", String.valueOf((now - con.getStartTime()) / 1000)));
			}
			
			// Display flight-specific stuff
			InfoMessage inf = con.getFlightInfo();
			if (inf != null) {
				ce.addContent(XMLUtils.createElement("flightCode", inf.getFlightCode()));
				ce.addContent(XMLUtils.createElement("flightEQ", inf.getEquipmentType()));
				ce.addContent(XMLUtils.createElement("airportD", inf.getAirportD().getICAO()));
				ce.addContent(XMLUtils.createElement("airportA", inf.getAirportA().getICAO()));
				if (inf.getNetwork() != null)
					ce.addContent(XMLUtils.createElement("network", inf.getNetwork().toString()));
			}
			
			// Display position if present
			PositionMessage pos = con.getPosition();
			if (pos != null) {
				ce.addContent(XMLUtils.createElement("lat", StringUtils.format(pos.getLatitude(), "##0.0000")));
				ce.addContent(XMLUtils.createElement("lng", StringUtils.format(pos.getLongitude(), "##0.0000")));
			}
			
			// Add connection specific stuff
			ce.addContent(XMLUtils.createElement("protocol", String.valueOf(con.getProtocolVersion())));
			ce.addContent(XMLUtils.createElement("clientVersion", String.valueOf(con.getVersion())));
			ce.addContent(XMLUtils.createElement("clientBuild", String.valueOf(con.getClientBuild())));
			ce.addContent(XMLUtils.createElement("beta", String.valueOf(con.getBeta())));
			ce.addContent(XMLUtils.createElement("starttime", Long.toHexString(con.getStartTime())));
			ce.addContent(XMLUtils.createElement("lastactivity", StringUtils.formatHex(con.getLastActivity())));
			if (isAdmin) {
				ConnectionStats cs = con.getTCPStatistics();
				ce.addContent(XMLUtils.createElement("input", String.valueOf(cs.getBytesIn())));
				ce.addContent(XMLUtils.createElement("output", String.valueOf(cs.getBytesOut())));
				ce.addContent(XMLUtils.createElement("msginput", String.valueOf(cs.getMsgsIn())));
				ce.addContent(XMLUtils.createElement("msgoutput", String.valueOf(cs.getMsgsOut())));
				ce.addContent(XMLUtils.createElement("remoteaddr", con.getRemoteAddr()));
				ce.addContent(XMLUtils.createElement("remotehost", con.getRemoteHost()));
				ce.addContent(XMLUtils.createElement("bytesSaved", String.valueOf(cs.getBytesSaved())));
				ce.addContent(XMLUtils.createElement("warnScore", String.valueOf(con.getWarningScore())));
			}

			e.addContent(ce);
		}
		
		return pe;
	}
}