// Copyright 2009, 2018, 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.beans.Pilot;
import org.deltava.beans.acars.TaxiTime;
import org.deltava.beans.wx.METAR;
import org.deltava.beans.navdata.Runway;
import org.deltava.beans.schedule.Airport;

import org.deltava.acars.message.*;

/**
 * A message to store airport weather and runway preferences.
 * @author Luke
 * @version 10.0
 * @since 2.6
 */

public class AirportInfoMessage extends DataResponseMessage<Runway> {
	
	private Airport _a;
	private METAR _wx;
	
	private TaxiTime _taxiTime;
	
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
	 * Returns the average airport taxi times.
	 * @return a TaxiTime
	 */
	public TaxiTime getTaxiTime() {
		return _taxiTime;
	}
	
	/**
	 * Updates the Airport.
	 * @param a the Airport
	 */
	public void setAirport(Airport a) {
		_a = a;
	}
	
	/**
	 * Updates the airport weather.
	 * @param wx the airport METAR
	 */
	public void setMETAR(METAR wx) {
		_wx = wx;
	}
	
	/**
	 * Updates the airport taxi times.
	 * @param tt a TaxiTime
	 */
	public void setTaxiTime(TaxiTime tt) {
		_taxiTime = tt;
	}
}