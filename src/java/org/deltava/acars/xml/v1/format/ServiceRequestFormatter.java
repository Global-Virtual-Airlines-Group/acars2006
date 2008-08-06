// Copyright 2007, 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import java.util.*;

import org.jdom.Element;

import org.deltava.acars.beans.FuelTank;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.dispatch.RequestMessage;

import org.deltava.util.XMLUtils;;

/**
 * An XML formatter for Dispatch service request messages.
 * @author Luke
 * @version 2.2
 * @since 2.0
 */

public class ServiceRequestFormatter extends ElementFormatter {

	/**
	 * Formats a DispatchRequestMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	public Element format(Message msg) {
		
		// Cast the message
		RequestMessage reqmsg = (RequestMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		Element e = initDataResponse(pe, reqmsg.getRequestTypeName());
		e.addContent(XMLUtils.createElement("originator", msg.getSenderID()));
		e.addContent(XMLUtils.createElement("id", Long.toHexString(msg.getID())));
		e.addContent(XMLUtils.createElement("routeValid", String.valueOf(reqmsg.isRouteValid())));
		
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
		for (Iterator<FuelTank> i = tankSizes.keySet().iterator(); i.hasNext(); ) {
			FuelTank t = i.next();
			int size= tankSizes.get(t).intValue();
			Element te = new Element("tank");
			te.setAttribute("name", t.getName());
			te.setAttribute("size", String.valueOf(size));
			tse.addContent(te);
		}
		
		e.addContent(tse);
		return pe;
	}
}