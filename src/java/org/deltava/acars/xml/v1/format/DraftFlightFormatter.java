// Copyright 2006, 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import java.util.Iterator;

import org.jdom.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.DraftPIREPMessage;

import org.deltava.beans.*;
import org.deltava.util.*;

/**
 * An XML Formatter for Draft Flight Report data messages.
 * @author Luke
 * @version 2.2
 * @since 1.0
 */

class DraftFlightFormatter extends ElementFormatter {

	/**
	 * Formats a DraftPIREPMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	public Element format(Message msg) {

		// Cast the message
		DraftPIREPMessage dfmsg = (DraftPIREPMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "pireps");
		for (Iterator<FlightReport> i = dfmsg.getResponse().iterator(); i.hasNext(); ) {
			FlightReport fr = i.next();
			
			// Build the PIREP element
			Element fe = new Element("pirep");
			fe.setAttribute("id", StringUtils.formatHex(fr.getID()));
			fe.setAttribute("airline", fr.getAirline().getCode());
			fe.setAttribute("number", StringUtils.format(fr.getFlightNumber(), "#000"));
			fe.setAttribute("leg", String.valueOf(fr.getLeg()));
			fe.setAttribute("assign", String.valueOf(fr.getDatabaseID(FlightReport.DBID_ASSIGN) != 0));
			fe.addContent(XMLUtils.createElement("eqType", fr.getEquipmentType()));
			fe.addContent(XMLUtils.createElement("airportA", fr.getAirportA().getICAO()));
			fe.addContent(XMLUtils.createElement("airportD", fr.getAirportD().getICAO()));
			fe.addContent(XMLUtils.createElement("remarks", fr.getRemarks(), true));
			if (fr.hasAttribute(FlightReport.ATTR_VATSIM))
				fe.setAttribute("network", OnlineNetwork.VATSIM.toString());
			else if (fr.hasAttribute(FlightReport.ATTR_IVAO))
				fe.setAttribute("network", OnlineNetwork.IVAO.toString());
			
			e.addContent(fe);
		}
		
		return pe;
	}
}