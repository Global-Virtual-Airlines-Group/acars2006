// Copyright 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.dispatch;

import org.deltava.beans.*;
import org.deltava.beans.schedule.GeoPosition;

import org.deltava.acars.message.DispatchMessage;

/**
 * An ACARS message to set a Dispatcher's service range.
 * @author Luke
 * @version 2.2
 * @since 2.2
 */

public class RangeMessage extends DispatchMessage {
	
	private GeoLocation _loc;
	private int _range;

	/**
	 * Initializes the Message.
	 * @param msgFrom the originating Dispatcher
	 */
	public RangeMessage(Pilot msgFrom) {
		super(DispatchMessage.DSP_RANGE, msgFrom);
	}

	/**
	 * Returns the service location.
	 * @return the location
	 */
	public GeoLocation getLocation() {
		return _loc;
	}
	
	/**
	 * Returns the service range.
	 * @return the range in miles
	 */
	public int getRange() {
		return _range;
	}

	/**
	 * Sets the service location.
	 * @param loc the location
	 */
	public void setLocation(GeoLocation loc) {
		_loc = new GeoPosition(loc);
	}
	
	/**
	 * Sets the service range.
	 * @param range the range in miles
	 */
	public void setRange(int range) {
		_range = Math.max(20, range);
	}
}