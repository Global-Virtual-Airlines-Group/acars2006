// Copyright 2004, 2009, 2010, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

import org.deltava.acars.message.TakeoffMessage;
import org.deltava.acars.message.data.AppInfoMessage;
import org.deltava.acars.message.data.DraftPIREPMessage;
import org.deltava.acars.message.dispatch.RoutePlotMessage;
import org.deltava.acars.message.mp.MPUpdateMessage;
import org.deltava.acars.message.viewer.*;

/**
 * V2 Protocol Message Formatter.
 * @author Luke
 * @version 3.6
 * @since 2.8
 */

public class Formatter extends org.deltava.acars.xml.v1.format.Formatter {

	@Override
	protected void init() {
		super.init();
		
		// Core formatters
		_eFormatters.put(TakeoffMessage.class, new TakeoffFormatter());
		_eFormatters.put(MPUpdateMessage.class, new MPUpdateFormatter());
		_eFormatters.put(DraftPIREPMessage.class, new DraftFlightFormatter());
		
		// Data formatters
		_eFormatters.put(AppInfoMessage.class, new AppInfoFormatter());
		
		// Dispatch formatters
		_eFormatters.put(RoutePlotMessage.class, new RoutePlotFormatter());
		
		// Flight Viewer formatters
		_eFormatters.put(AcceptMessage.class, new ViewAcceptFormatter());
		_eFormatters.put(CancelMessage.class, new ViewCancelFormatter());
		_eFormatters.put(RequestMessage.class, new ViewRequestFormatter());
	}

	/**
	 * Initializes the Message Formatter.
	 */
	public Formatter() {
		super(1);
	}
}