// Copyright 2008, 2009, 2010, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

import java.util.*;

import org.jdom.Element;

import org.deltava.acars.beans.MPUpdate;

import org.deltava.acars.message.*;
import org.deltava.acars.message.mp.MPUpdateMessage;

import org.deltava.acars.xml.XMLElementFormatter;

import org.deltava.util.*;

/**
 * An XML formatter for multi-player position update messages.
 * @author Luke
 * @version 4.1
 * @since 2.2
 */

public class MPUpdateFormatter extends XMLElementFormatter {

	/**
	 * Formats an MPUpdateMessage bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	@Override
	public Element format(Message msg) {
		
		// Cast the message
		MPUpdateMessage mpmsg = (MPUpdateMessage) msg;
		
		// Create the element
		Element pe = initResponse(msg.getType());
		if (mpmsg.isClear())
			pe.setAttribute("clear", "true");
		
		// Add position updates
		for (Iterator<MPUpdate> i = mpmsg.getUpdates().iterator(); i.hasNext(); ) {
			MPUpdate upd = i.next();
			LocationMessage lmsg = upd.getLocation();
			
			// Build the element
			Element le = new Element("pos");
			le.setAttribute("id", upd.getHexID()); // pilot's database ID!
			le.setAttribute("flightID", StringUtils.formatHex(upd.getFlightID()));
			le.setAttribute("lat", StringUtils.format(lmsg.getLatitude(), "##0.000000"));
			le.setAttribute("lon", StringUtils.format(lmsg.getLongitude(), "##0.000000"));
			le.setAttribute("v", String.valueOf(lmsg.getVspeed()));
			le.setAttribute("f", String.valueOf(lmsg.getFlags()));
			le.setAttribute("h", String.valueOf(lmsg.getHeading()));
			le.setAttribute("g", String.valueOf(lmsg.getGspeed()));
			le.setAttribute("a", String.valueOf(lmsg.getDoubleAltitude()));
			le.setAttribute("p", String.valueOf(lmsg.getPitch()));
			le.setAttribute("b", String.valueOf(lmsg.getBank()));
			le.setAttribute("fl", String.valueOf(lmsg.getFlaps()));
			le.setAttribute("l", String.valueOf(lmsg.getLights()));
			
			// Add transponder data if we have it
			if (lmsg instanceof PositionMessage) {
				PositionMessage pmsg = (PositionMessage) lmsg;
				le.setAttribute("tx", String.valueOf(pmsg.getTXActive()));
				le.setAttribute("txCode", String.valueOf(pmsg.getTXCode()));
			}
			
			// Add livery data
			if (mpmsg.hasLivery()) {
				le.addContent(XMLUtils.createElement("eqType", upd.getEquipmentType()));
				le.addContent(XMLUtils.createElement("airline", upd.getAirlineCode()));
				le.addContent(XMLUtils.createElement("livery", upd.getLiveryCode()));
			}
			
			le.addContent(XMLUtils.createElement("callsign", upd.getCallsign()));
			pe.addContent(le);
		}
		
		return pe;
	}
}