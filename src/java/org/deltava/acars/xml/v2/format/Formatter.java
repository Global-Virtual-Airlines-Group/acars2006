// Copyright 2004, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.format;

import org.deltava.acars.message.TakeoffMessage;
import org.deltava.acars.message.data.DraftPIREPMessage;
import org.deltava.acars.message.mp.MPUpdateMessage;
import org.deltava.acars.message.viewer.*;

/**
 * V2 Protocol Message Formatter.
 * @author Luke
 * @version 2.8
 * @since 2.8
 */

public class Formatter extends org.deltava.acars.xml.v1.format.Formatter {

	protected void init() {
		super.init();
		
		// Core formatters
		_eFormatters.put(TakeoffMessage.class, new TakeoffFormatter());
		_eFormatters.put(MPUpdateMessage.class, new MPUpdateFormatter());
		_eFormatters.put(DraftPIREPMessage.class, new DraftFlightFormatter());
		
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