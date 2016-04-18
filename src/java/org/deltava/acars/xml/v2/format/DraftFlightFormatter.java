// Copyright 2006, 2008, 2009, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

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

class DraftFlightFormatter extends org.deltava.acars.xml.XMLElementFormatter {

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
			fe.addContent(XMLUtils.createElement("lf", StringUtils.format(fr.getLoadFactor(), "0.000")));
			if (fr.getNetwork() != null)
				fe.addContent(XMLUtils.createElement("network", fr.getNetwork().toString()));
			if (!StringUtils.isEmpty(fr.getRoute()))
				fe.addContent(XMLUtils.createElement("route", fr.getRoute(), true));
			
			// Add scheduled departure/arrival times
			if (fr instanceof DraftFlightReport) {
				DraftFlightReport dfr = (DraftFlightReport) fr;
				if (dfr.getTimeD() != null)
					fe.addContent(XMLUtils.createElement("timeD", StringUtils.format(dfr.getTimeD(), "HH:mm")));
				if (dfr.getTimeA() != null)
					fe.addContent(XMLUtils.createElement("timeA", StringUtils.format(dfr.getTimeA(), "HH:mm")));
			}
			
			e.addContent(fe);
		}
		
		return pe;
	}
}