// Copyright 2009, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.beans.Pilot;
import org.deltava.beans.wx.METAR;
import org.deltava.beans.navdata.Runway;
import org.deltava.beans.schedule.Airport;

import org.deltava.acars.message.*;

/**
 * A message to store airport weather and runway preferences.
 * @author Luke
 * @version 8.4
 * @since 2.6
 */

public class AirportInfoMessage extends DataResponseMessage<Runway> {
	
	private Airport _a;
	private METAR _wx;
	
	/**
	 * Instantiates the Message.
	 * @param msgFrom the originating Pilot
	 * @param parentID the parent messge ID
	 */
	public AirportInfoMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, DataRequest.APINFO, parentID);
	}
	
	/**
	 * Returns the Airport.
	 * @return the Airport bean
	 */
	public Airport getAirport() {
		return _a;
	}

	/**
	 * Returns the current airport weather.
	 * @return the airport METAR
	 */
	public METAR getMETAR() {
		return _wx;
	}
	
	/**
	 * Updates the Airport.
	 * @param a the Airport bean
	 */
	public void setAirport(Airport a) {
		_a = a;
	}
	
	/**
	 * Updates the airporrt weather.
	 * @param wx the airport METAR
	 */
	public void setMETAR(METAR wx) {
		_wx = wx;
	}
}