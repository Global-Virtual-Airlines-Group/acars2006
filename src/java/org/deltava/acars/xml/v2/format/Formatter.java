// Copyright 2004, 2009, 2010, 2011, 2012, 2013, 2015, 2017, 2018, 2022, 2023, 2024 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

import org.deltava.acars.message.*;
import org.deltava.acars.message.data.*;
import org.deltava.acars.message.dispatch.RoutePlotMessage;

/**
 * V2 Protocol Message Formatter.
 * @author Luke
 * @version 11.3
 * @since 2.8
 */

public class Formatter extends org.deltava.acars.xml.v1.format.Formatter {

	/**
	 * Constructor.
	 */
	public Formatter() {
		super(2);
	}

	/**
	 * Initializes the Message Formatter.
	 */
	@Override
	protected void init() {
		super.init();
		
		// Core formatters
		_eFormatters.put(TakeoffMessage.class, new TakeoffFormatter());
		_eFormatters.put(DraftPIREPMessage.class, new DraftFlightFormatter());
		_eFormatters.put(UpdateIntervalMessage.class, new UpdateIntervalFormatter());
		_eFormatters.put(DiagnosticMessage.class, new DiagnosticFormatter());
		
		// Data formatters
		_eFormatters.put(AlternateAirportMessage.class, new AlternateAirportFormatter());
		_eFormatters.put(AppInfoMessage.class, new AppInfoFormatter());
		_eFormatters.put(AirportMessage.class, new AirportFormatter());
		_eFormatters.put(RunwayListMessage.class, new RunwayListFormatter());
		_eFormatters.put(GateMessage.class, new GateFormatter());
		_eFormatters.put(ATISMessage.class, new ATISFormatter());
		
		// Dispatch formatters
		_eFormatters.put(RoutePlotMessage.class, new RoutePlotFormatter());
	}
}