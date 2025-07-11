// Copyright 2007, 2008, 2010, 2011, 2012, 2018, 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import java.util.*;

import org.jdom2.Element;

import org.deltava.beans.schedule.FuelTank;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.dispatch.RequestMessage;

import org.deltava.util.XMLUtils;

/**
 * An XML formatter for Dispatch service request messages.
 * @author Luke
 * @version 10.0
 * @since 2.0
 */

public class ServiceRequestFormatter extends ElementFormatter {

	/**
	 * Formats a DispatchRequestMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {
		
		// Cast the message
		RequestMessage reqmsg = (RequestMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, reqmsg.getRequestType().getCode());
		e.setAttribute("id", String.valueOf(reqmsg.getID()));
		e.addContent(XMLUtils.createElement("originator", msg.getSenderID()));
		e.addContent(XMLUtils.createElement("id", Long.toHexString(msg.getID())));
		e.addContent(XMLUtils.createElement("sim", reqmsg.getSimulator().name()));
		e.addContent(XMLUtils.createElement("routeValid", String.valueOf(reqmsg.isRouteValid())));
		e.addContent(XMLUtils.createElement("etopsWarn", String.valueOf(reqmsg.getETOPSWarning())));
		
		// Add MGW and ZFW
		e.addContent(XMLUtils.createElement("maxweight", String.valueOf(reqmsg.getMaxWeight())));
		e.addContent(XMLUtils.createElement("emptyweight", String.valueOf(reqmsg.getZeroFuelWeight())));
		
		// Add airport/airline/equipment data
		e.addContent(XMLUtils.createElement("eqType", reqmsg.getEquipmentType()));
		e.addContent(XMLUtils.createElement("airline", reqmsg.getAirline().getCode()));
		e.addContent(XMLUtils.createElement("airportD", reqmsg.getAirportD().getICAO()));
		e.addContent(XMLUtils.createElement("airportA", reqmsg.getAirportA().getICAO()));
		if (reqmsg.getAirportL() != null)
			e.addContent(XMLUtils.createElement("airportL", reqmsg.getAirportL().getICAO()));
		
		// Add closest gate
		XMLUtils.addIfPresent(e, formatGate(reqmsg.getClosestGate(), "gate"));
		
		// Add tank capacity data
		Element tse = new Element("tanks");
		e.addContent(tse);
		for (Map.Entry<FuelTank, Integer> fte : reqmsg.getTankSizes().entrySet()) {
			Element te = new Element("tank");
			te.setAttribute("name", fte.getKey().getName());
			te.setAttribute("size", String.valueOf(fte.getValue()));
			tse.addContent(te);
		}
		
		// Add arrival gates
		Element age = new Element("gates");
		reqmsg.getArrivalGates().forEach(g -> age.addContent(formatGate(g, "gate")));
		return pe;
	}
}