// Copyright 2004, 2005, 2006, 2007, 2008, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import java.util.*;

import org.jdom.Element;

import org.deltava.acars.message.*;
import org.deltava.acars.message.data.*;
import org.deltava.acars.message.dispatch.*;
import org.deltava.acars.message.mp.MPUpdateMessage;
import org.deltava.acars.xml.*;

/**
 * V1 Protocol Message Formatter.
 * @author Luke
 * @version 2.6
 * @since 1.0
 */

public class Formatter extends MessageFormatter {
	
	private final Map<Class<? extends Message>, ElementFormatter> _eFormatters = 
		new HashMap<Class<? extends Message>, ElementFormatter>() {
		{
			put(QuitMessage.class, new NullFormatter());	
			put(AcknowledgeMessage.class, new AckFormatter());
			put(TextMessage.class, new TextMessageFormatter());
			put(SystemTextMessage.class, new SystemMessageFormatter());
			put(DiagnosticMessage.class, new DiagnosticFormatter());
			
			// Data response formatters
			put(AircraftMessage.class, new AircraftFormatter());
			put(AirlineMessage.class, new AirlineFormatter());
			put(AirportMessage.class, new AirportFormatter());
			put(ChartsMessage.class, new ChartsFormatter());
			put(ConnectionMessage.class, new ConnectionFormatter());
			put(ControllerMessage.class, new ControllerFormatter());
			put(DraftPIREPMessage.class, new DraftFlightFormatter());
			put(GenericMessage.class, new GenericFormatter());
			put(NavigationDataMessage.class, new NavinfoFormatter());
			put(PilotMessage.class, new PilotFormatter());
			put(ScheduleMessage.class, new ScheduleFormatter());
			put(TS2ServerMessage.class, new TS2ServerFormatter());
			put(TerminalRouteMessage.class, new TerminalRouteFormatter());
			put(OceanicTrackMessage.class, new OceanicRouteFormatter());
			put(LiveryMessage.class, new LiveryFormatter());
			put(WXMessage.class, new WeatherFormatter());
			put(AirportInfoMessage.class, new AirportInfoFormatter());
			
			// Dispatch response formatters
			put(FlightDataMessage.class, new DispatchInfoFormatter());
			put(CancelMessage.class, new DispatchCancelFormatter());
			put(RequestMessage.class, new ServiceRequestFormatter());
			put(RouteInfoMessage.class, new DispatchRouteFormatter());
			put(CompleteMessage.class, new ServiceCompleteFormatter());
			put(ProgressResponseMessage.class, new DispatchProgressFormatter());
			
			// MP response formatters
			put(MPUpdateMessage.class, new MPUpdateFormatter());
		}
	};

	/**
	 * Initializes the Message Formatter.
	 */
	public Formatter() {
		super(1);
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
		if ((msg instanceof DataResponseMessage<?>) && (e != null))
			e.setAttribute("id", Long.toHexString(((DataResponseMessage<?>) msg).getParentID()).toUpperCase());
			
		return e;
	}
}