// Copyright 2006, 2008, 2009, 2010, 2012, 2014 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import org.jdom2.Element;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.data.DraftPIREPMessage;

import org.deltava.beans.flight.*;
import org.deltava.util.*;

/**
 * An XML Formatter for Draft Flight Report data messages.
 * @author Luke
 * @version 5.3
 * @since 1.0
 */

class DraftFlightFormatter extends ElementFormatter {

	/**
	 * Formats a DraftPIREPMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {

		// Cast the message
		DraftPIREPMessage dfmsg = (DraftPIREPMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, "pireps");
		for (FlightReport fr : dfmsg.getResponse()) {
			Element fe = new Element("pirep");
			fe.setAttribute("id", StringUtils.formatHex(fr.getID()));
			fe.setAttribute("airline", fr.getAirline().getCode());
			fe.setAttribute("number", StringUtils.format(fr.getFlightNumber(), "#000"));
			fe.setAttribute("leg", String.valueOf(fr.getLeg()));
			fe.setAttribute("assign", String.valueOf(fr.getDatabaseID(DatabaseID.ASSIGN) != 0));
			fe.addContent(XMLUtils.createElement("eqType", fr.getEquipmentType()));
			fe.addContent(XMLUtils.createElement("airportA", fr.getAirportA().getICAO()));
			fe.addContent(XMLUtils.createElement("airportD", fr.getAirportD().getICAO()));
			fe.addContent(XMLUtils.createElement("remarks", fr.getRemarks(), true));
			if (fr.getNetwork() != null)
				fe.setAttribute("network", fr.getNetwork().toString());
			
			e.addContent(fe);
		}
		
		return pe;
	}
}