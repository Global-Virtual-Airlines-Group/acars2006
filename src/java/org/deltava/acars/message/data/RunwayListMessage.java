// Copyright 2018, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.beans.Pilot;
import org.deltava.beans.navdata.Runway;
import org.deltava.beans.schedule.Airport;

import org.deltava.acars.message.*;

/**
 * An ACARS data response message to store Runway information. 
 * @author Luke
 * @version 8.6
 * @since 8.4
 */

public class RunwayListMessage extends DataResponseMessage<Runway> {
	
	private Airport _aD;
	private Airport _aA;
	
	private final boolean _isPopular;
	
	/**
	 * Instantiates the Message.
	 * @param msgFrom the originating Pilot
	 * @param parentID the request message ID
	 * @param isPopular TRUE if a popular runway list, otherwise FALSE
	 */
	public RunwayListMessage(Pilot msgFrom, long parentID, boolean isPopular) {
		super(msgFrom, DataRequest.RUNWAYS, parentID);
		_isPopular = isPopular;
	}
	
	/**
	 * Returns whether this is a popular runway list.
	 * @return TRUE if popular runways, otherwise FALSE
	 */
	public boolean isPopular() {
		return _isPopular;
	}

	/**
	 * Returns the departure Airport.
	 * @return the Airport
	 */
	public Airport getAirportD() {
		return _aD;
	}
	
	/**
	 * Returns the arrival Airport.
	 * @return the Airport
	 */
	public Airport getAirportA() {
		return _aA;
	}

	/**
	 * Updates the departure Airport.
	 * @param a the Airport
	 */
	public void setAirportD(Airport a) {
		_aD = a;
	}

	/**
	 * Updates the arrival Airport.
	 * @param a the Airport
	 */
	public void setAirportA(Airport a) {
		_aA = a;
	}
}