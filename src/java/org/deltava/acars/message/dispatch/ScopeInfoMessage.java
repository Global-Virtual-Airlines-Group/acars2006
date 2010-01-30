// Copyright 2010 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.dispatch;

import org.deltava.beans.*;
import org.deltava.beans.schedule.GeoPosition;

import org.deltava.acars.message.DispatchMessage;

/**
 * An ACARS message to transmit dispatch radar scope parameters.
 * @author Luke
 * @version 3.0
 * @since 3.0
 */

public class ScopeInfoMessage extends DispatchMessage implements GeoLocation {
	
	private GeoLocation _ctr = new GeoPosition(0, 0);
	private int _range;

	/**
	 * Initializes the Message. 
	 * @param msgFrom
	 */
	public ScopeInfoMessage(Pilot msgFrom) {
		super(DispatchMessage.DSP_SCOPEINFO, msgFrom);
	}
	
	/**
	 * Returns the scope center latitude.
	 */
	public double getLatitude() {
		return _ctr.getLatitude();
	}
	
	/**
	 * Returns the scope center longitude.
	 */
	public double getLongitude() {
		return _ctr.getLongitude();
	}
	
	/**
	 * Returns the radar scope range.
	 * @return the range in miles
	 */
	public int getRange() {
		return _range;
	}

	/**
	 * Updates the radar scope center.
	 * @param loc the center
	 */
	public void setCenter(GeoLocation loc) {
		_ctr = new GeoPosition(loc);
	}
	
	/**
	 * Updates the radar scope range.
	 * @param range the range in miles
	 */
	public void setRange(int range) {
		_range = Math.max(0, range);
	}
}