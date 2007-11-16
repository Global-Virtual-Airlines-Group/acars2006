// Copyright 2004, 2005, 2006, 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1;

import java.util.*;

import org.jdom.Element;

import org.deltava.acars.message.*;
import org.deltava.acars.message.data.*;
import org.deltava.acars.xml.*;

/**
 * V1 Protocol Message Formatter.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class Formatter extends MessageFormatter {
	
	private final Map<Class, ElementFormatter> _eFormatters = new HashMap<Class, ElementFormatter>();

	/**
	 * Initializes the Message Formatter.
	 */
	public Formatter() {
		super(1);
		_eFormatters.put(QuitMessage.class, new NullFormatter());
		_eFormatters.put(AcknowledgeMessage.class, new AckFormatter());
		_eFormatters.put(DispatchMessage.class, new DispatchFormatter());
		_eFormatters.put(TextMessage.class, new TextMessageFormatter());
		_eFormatters.put(SystemTextMessage.class, new SystemMessageFormatter());
		_eFormatters.put(DiagnosticMessage.class, new DiagnosticFormatter());
		
		// Data response formatters
		_eFormatters.put(AircraftMessage.class, new AircraftFormatter());
		_eFormatters.put(AirlineMessage.class, new AirlineFormatter());
		_eFormatters.put(AirportMessage.class, new AirportFormatter());
		_eFormatters.put(ChartsMessage.class, new ChartsFormatter());
		_eFormatters.put(ConnectionMessage.class, new ConnectionFormatter());
		_eFormatters.put(ControllerMessage.class, new ControllerFormatter());
		_eFormatters.put(DraftPIREPMessage.class, new DraftFlightFormatter());
		_eFormatters.put(GenericMessage.class, new GenericFormatter());
		_eFormatters.put(NavigationDataMessage.class, new NavinfoFormatter());
		_eFormatters.put(PilotMessage.class, new PilotFormatter());
		_eFormatters.put(ScheduleMessage.class, new ScheduleFormatter());
		_eFormatters.put(TS2ServerMessage.class, new TS2ServerFormatter());
		_eFormatters.put(TerminalRouteMessage.class, new TerminalRouteFormatter());
	}

	/**
	 * Formats a Message bean into an XML element.
	 * @param msg the Message bean
	 * @return an XML element
	 * @throws XMLException if a formatting error occurs
	 */
	public Element format(Message msg) throws XMLException {

		// Get the element formatter to use
		ElementFormatter efmt = _eFormatters.get(msg.getClass());
		if (efmt == null) {
			log.warn("Cannot format " + msg.getClass().getSimpleName());
			return null;
		}

		// Format the message
		Element e = efmt.format(msg);
		if ((msg instanceof DataResponseMessage) && (e != null))
			e.setAttribute("id", Long.toHexString(((DataResponseMessage) msg).getParentID()).toUpperCase());
			
		return e;
	}
}