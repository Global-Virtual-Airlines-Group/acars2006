// Copyright 2010, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.dispatch;

import org.deltava.beans.*;
import org.deltava.beans.schedule.GeoPosition;

import org.deltava.acars.message.DispatchMessage;

/**
 * An ACARS message to transmit dispatch radar scope parameters.
 * @author Luke
 * @version 3.6
 * @since 3.0
 */

public class ScopeInfoMessage extends DispatchMessage implements GeoLocation {
	
	private GeoLocation _ctr = new GeoPosition(0, 0);
	private int _range;
	
	private OnlineNetwork _network;
	private boolean _allTraffic;
	
	private String _callSign;
	private String _freq;
	
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
	 * Returns the Online Network to monitor.
	 * @return an OnlineNetwork, or null for Offline 
	 */
	public OnlineNetwork getNetwork() {
		return _network;
	}
	
	/**
	 * Returns whether to display all traffic.
	 * @return TRUE for all traffic, otherwise FALSE
	 */
	public boolean getAllTraffic() {
		return _allTraffic;
	}
	
	/**
	 * Returns the callsign if operating as an ATC client.
	 * @return the callsign
	 */
	public String getCallsign() {
		return _callSign;
	}
	
	/**
	 * Returns the frequency if operating as an ATC client.
	 * @return the frequency
	 */
	public String getFrequency() {
		return _freq;
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
	
	/**
	 * Sets the Online Network to monitor.
	 * @param net an OnlineNetwork, or null for Offline 
	 */
	public void setNetwork(OnlineNetwork net) {
		_network = net;
	}
	
	/**
	 * Sets whether to display all traffic.
	 * @param doAll TRUE for all traffic, otherwise FALSE
	 */
	public void setAllTraffic(boolean doAll) {
		_allTraffic = doAll;
	}
	
	/**
	 * Updates the position callsign if operating as an ATC client.
	 * @param cs the callsign
	 */
	public void setCallsign(String cs) {
		_callSign = (cs == null) ? null : cs.toUpperCase();
	}
	
	/**
	 * Updates the frequency if operating as an ATC client.
	 * @param freq the frequency
	 */
	public void setFrequency(String freq) {
		_freq = freq;
	}
}