// Copyright 2007, 2008, 2010, 2011, 2012 Global Virtual Airlines Group. All Rights Reserved.
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
 * @version 4.2
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
		Element e = initDataResponse(pe, reqmsg.getRequestTypeName());
		e.setAttribute("id", String.valueOf(reqmsg.getID()));
		e.addContent(XMLUtils.createElement("originator", msg.getSenderID()));
		e.addContent(XMLUtils.createElement("id", Long.toHexString(msg.getID())));
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
		
		// Add tank capacity data
		Element tse = new Element("tanks");
		Map<FuelTank, Integer> tankSizes = reqmsg.getTankSizes();
		for (Iterator<Map.Entry<FuelTank, Integer>> i = tankSizes.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry<FuelTank, Integer> fte = i.next();
			Element te = new Element("tank");
			te.setAttribute("name", fte.getKey().getName());
			te.setAttribute("size", String.valueOf(fte.getValue()));
			tse.addContent(te);
		}
		
		e.addContent(tse);
		return pe;
	}
}