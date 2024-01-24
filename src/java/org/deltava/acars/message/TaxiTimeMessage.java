// Copyright 2024 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * An ACARS message send flight taxi time data.
 * @author Luke
 * @version 11.2
 * @since 11.2
 */

public class TaxiTimeMessage extends AbstractMessage {
	
	private int _flightID;
	private int _taxiIn;
	private int _taxiOut;

	/**
	 * Creates the bean.
	 * @param msgFrom the Author 
	 */
	public TaxiTimeMessage(Pilot msgFrom) {
		super(MessageType.TAXI, msgFrom);
	}
	
	/**
	 * Returns the ACARS flight ID.
	 * @return the ID
	 */
	public int getFlightID() {
		return _flightID;
	}

	/**
	 * Returns the inbound taxi time.
	 * @return the time in seconds
	 */
	public int getInboundTaxiTime() {
		return _taxiIn;
	}
	
	/**
	 * Returns the outbound taxi time.
	 * @return the time in seconds
	 */
	public int getOutboundTaxiTime() {
		return _taxiOut;
	}
	
	/**
	 * Updates the ACARS flight ID.
	 * @param id the ID
	 */
	public void setFlightID(int id) {
		_flightID = id;
	}
	
	/**
	 * Updates the inbound taxi time.
	 * @param secs the time in seconds
	 */
	public void setInboundTaxiTime(int secs) {
		_taxiIn = Math.max(0, secs);
	}
	
	/**
	 * Updates the outbound taxi time.
	 * @param secs the time in seconds
	 */
	public void setOutboundTaxiTime(int secs) {
		_taxiOut = Math.max(0, secs);
	}
}