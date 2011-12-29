// Copyright 2004, 2009, 2010, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

import org.deltava.acars.message.*;
import org.deltava.acars.message.data.*;
import org.deltava.acars.message.dispatch.RoutePlotMessage;
import org.deltava.acars.message.mp.*;

/**
 * V2 Protocol Message Formatter.
 * @author Luke
 * @version 4.1
 * @since 2.8
 */

public class Formatter extends org.deltava.acars.xml.v1.format.Formatter {

	/**
	 * Initializes the Message Formatter.
	 */
	public Formatter() {
		super(2);
	}

	@Override
	protected void init() {
		super.init();
		
		// Core formatters
		_eFormatters.put(TakeoffMessage.class, new TakeoffFormatter());
		_eFormatters.put(DraftPIREPMessage.class, new DraftFlightFormatter());
		_eFormatters.put(UpdateIntervalMessage.class, new UpdateIntervalFormatter());
		
		// Data formatters
		_eFormatters.put(AppInfoMessage.class, new AppInfoFormatter());
		_eFormatters.put(ChannelListMessage.class, new ChannelListFormatter());
		
		// Dispatch formatters
		_eFormatters.put(RoutePlotMessage.class, new RoutePlotFormatter());
		
		// MP formatters
		_eFormatters.put(MPUpdateMessage.class, new MPUpdateFormatter());
		_eFormatters.put(RemoveMessage.class, new MPRemoveFormatter());
	}
}