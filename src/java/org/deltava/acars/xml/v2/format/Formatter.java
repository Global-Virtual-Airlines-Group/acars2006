// Copyright 2004, 2009, 2010, 2011, 2012, 2013, 2015 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

import org.deltava.acars.message.*;
import org.deltava.acars.message.data.*;
import org.deltava.acars.message.dispatch.RoutePlotMessage;
import org.deltava.acars.message.mp.*;

/**
 * V2 Protocol Message Formatter.
 * @author Luke
 * @version 6.2
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
		_eFormatters.put(VoicePingIntervalMessage.class, new UpdateIntervalFormatter());
		
		// Data formatters
		_eFormatters.put(AlternateAirportMessage.class, new AlternateAirportFormatter());
		_eFormatters.put(AppInfoMessage.class, new AppInfoFormatter());
		_eFormatters.put(ChannelListMessage.class, new ChannelListFormatter());
		_eFormatters.put(AirportMessage.class, new AirportFormatter());
		_eFormatters.put(IATACodeMessage.class, new IATACodeFormatter());
		
		// Dispatch formatters
		_eFormatters.put(RoutePlotMessage.class, new RoutePlotFormatter());
		
		// MP formatters
		_eFormatters.put(MPUpdateMessage.class, new MPUpdateFormatter());
		_eFormatters.put(RemoveMessage.class, new MPRemoveFormatter());
	}
}