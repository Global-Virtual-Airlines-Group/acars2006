// Copyright 2004, 2005, 2006, 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import java.util.Iterator;

import org.jdom.Element;

import org.deltava.acars.beans.ACARSConnection;

import org.deltava.acars.message.*;
import org.deltava.acars.message.data.ConnectionMessage;

import org.deltava.beans.Pilot;

import org.deltava.util.*;

/**
 * An XML Formatter for ACARS Connection data messages.
 * @author Luke
 * @version 2.2
 * @since 1.0
 */

class ConnectionFormatter extends ElementFormatter {

	/**
	 * Formats a ConnectionMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	public Element format(Message msg) {

		// Cast the message
		ConnectionMessage cmsg = (ConnectionMessage) msg;
		Pilot owner = cmsg.getSender();
		boolean isAdmin = (owner != null) && owner.isInRole("HR");

		// Create the parent elements
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, (cmsg.getRequestType() == DataMessage.REQ_USRLIST) ? "pilotlist" : "addpilots");
		for (Iterator<ACARSConnection> i = cmsg.getResponse().iterator(); i.hasNext(); ) {
			ACARSConnection con = i.next();
			
			// Create the element
			Element ce = new Element("Pilot");
			if (con.isAuthenticated()) {
				Pilot usr = con.getUser();
				ce.setAttribute("id", usr.getPilotCode());
				ce.setAttribute("dbID", Integer.toHexString(usr.getID()));
				ce.addContent(XMLUtils.createElement("firstname", usr.getFirstName()));
				ce.addContent(XMLUtils.createElement("lastname", usr.getLastName()));
				ce.addContent(XMLUtils.createElement("name", usr.getName()));
				ce.addContent(XMLUtils.createElement("eqtype", usr.getEquipmentType()));
				ce.addContent(XMLUtils.createElement("rank", usr.getRank()));
				ce.addContent(XMLUtils.createElement("hours", String.valueOf(usr.getHours())));
				ce.addContent(XMLUtils.createElement("legs", String.valueOf(usr.getLegs())));
				ce.addContent(XMLUtils.createElement("joinedOn", StringUtils.format(usr.getCreatedOn(), "MMMM dd, yyyy")));
				ce.addContent(XMLUtils.createElement("isBusy", String.valueOf(con.getUserBusy())));
				ce.addContent(XMLUtils.createElement("isDispatch", String.valueOf(con.getIsDispatch())));
				ce.addContent(XMLUtils.createElement("isHidden", String.valueOf(con.getUserHidden())));
				ce.addContent(XMLUtils.createElement("roles", StringUtils.listConcat(usr.getRoles(), ",")));
			}
			
			// Display flight-specific stuff
			InfoMessage inf = con.getFlightInfo();
			if (inf != null) {
				ce.addContent(XMLUtils.createElement("flightCode", inf.getFlightCode()));
				ce.addContent(XMLUtils.createElement("flightEQ", inf.getEquipmentType()));
				ce.addContent(XMLUtils.createElement("airportD", inf.getAirportD().getICAO()));
				ce.addContent(XMLUtils.createElement("airportA", inf.getAirportA().getICAO()));
			}
			
			// Add connection specific stuff
			ce.addContent(XMLUtils.createElement("protocol", String.valueOf(con.getProtocolVersion())));
			ce.addContent(XMLUtils.createElement("clientBuild", String.valueOf(con.getClientVersion())));
			ce.addContent(XMLUtils.createElement("beta", String.valueOf(con.getBeta())));
			ce.addContent(XMLUtils.createElement("starttime", Long.toHexString(con.getStartTime())));
			ce.addContent(XMLUtils.createElement("lastactivity", StringUtils.formatHex(con.getLastActivity())));
			ce.addContent(XMLUtils.createElement("input", String.valueOf(con.getBytesIn())));
			ce.addContent(XMLUtils.createElement("output", String.valueOf(con.getBytesOut())));
			ce.addContent(XMLUtils.createElement("msginput", String.valueOf(con.getMsgsIn())));
			ce.addContent(XMLUtils.createElement("msgoutput", String.valueOf(con.getMsgsOut())));
			if (isAdmin) {
				ce.addContent(XMLUtils.createElement("remoteaddr", con.getRemoteAddr()));
				ce.addContent(XMLUtils.createElement("remotehost", con.getRemoteHost()));
			}

			e.addContent(ce);
		}
		
		return pe;
	}
}