// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2012, 2016, 2019, 2021, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import org.jdom2.Element;

import org.deltava.acars.message.*;
import org.deltava.acars.message.data.*;
import org.deltava.acars.message.dispatch.*;
import org.deltava.acars.xml.*;

/**
 * V1 Protocol Message Formatter.
 * @author Luke
 * @version 11.1
 * @since 1.0
 */

public class Formatter extends XMLMessageFormatter {

	protected void init() {
		_eFormatters.put(QuitMessage.class, new NullFormatter());
		_eFormatters.put(AcknowledgeMessage.class, new AckFormatter());
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
		_eFormatters.put(NavigationDataMessage.class, new NavinfoFormatter());
		_eFormatters.put(PilotMessage.class, new PilotFormatter());
		_eFormatters.put(ScheduleMessage.class, new ScheduleFormatter());
		_eFormatters.put(OceanicTrackMessage.class, new OceanicRouteFormatter());
		_eFormatters.put(WXMessage.class, new WeatherFormatter());
		_eFormatters.put(AirportInfoMessage.class, new AirportInfoFormatter());

		// Dispatch response formatters
		_eFormatters.put(FlightDataMessage.class, new DispatchInfoFormatter());
		_eFormatters.put(CancelMessage.class, new DispatchCancelFormatter());
		_eFormatters.put(RequestMessage.class, new ServiceRequestFormatter());
		_eFormatters.put(RouteInfoMessage.class, new DispatchRouteFormatter());
		_eFormatters.put(CompleteMessage.class, new ServiceCompleteFormatter());
		_eFormatters.put(ProgressResponseMessage.class, new DispatchProgressFormatter());
	}

	/**
	 * Initializes the Message Formatter.
	 */
	public Formatter() {
		this(1);
	}
	
	/**
	 * Initializes the Message Formatter, for use by subclasses.
	 * @param version the protocol version
	 */
	protected Formatter(int version) {
		super(version);
		init();
	}

	/**
	 * Formats a Message bean into an XML element.
	 * @param msg the Message bean
	 * @return an XML element
	 * @throws XMLException if a formatting error occurs
	 */
	@Override
	public Element format(Message msg) throws XMLException {

		// Get the element formatter to use
		XMLElementFormatter efmt = _eFormatters.get(msg.getClass());
		if (efmt == null) {
			log.warn("Cannot format {}", msg.getClass().getName());
			return null;
		}

		// Format the message
		Element e = efmt.format(msg);
		if ((msg instanceof DataResponseMessage<?> drmsg) && (e != null)) {
			e.setAttribute("id", Long.toHexString(drmsg.getParentID()).toUpperCase());
			e.setAttribute("maxAge", String.valueOf(drmsg.getMaxAge()));
		}

		return e;
	}
}