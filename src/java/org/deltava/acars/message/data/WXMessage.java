// Copyright 2008, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.beans.Pilot;
import org.deltava.beans.schedule.Airport;
import org.deltava.beans.wx.*;

import org.deltava.acars.message.*;

/**
 * An ACARS data response message to store weather information.
 * @author Luke
 * @version 8.4
 * @since 2.2
 */

public class WXMessage extends DataResponseMessage<WeatherDataBean> {
	
	private Airport _a;
	
	/**
	 * Instantiates the Message.
	 * @param msgFrom the originating Pilot
	 * @param parentID the request message ID
	 */
	public WXMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, DataRequest.WX, parentID);
	}
	
	/**
	 * Returns the airport associated with this weather data.
	 * @return the Airport bean
	 */
	public Airport getAirport() {
		return _a;
	}
	
	/**
	 * Updates the Airport associated with this weather data. 
	 * @param a the Airport bean
	 */
	public void setAirport(Airport a) {
		_a = a;
	}
}