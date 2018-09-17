// Copyright 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.beans.Pilot;
import org.deltava.beans.navdata.Runway;
import org.deltava.beans.schedule.Airport;

import org.deltava.acars.message.*;

/**
 * An ACARS data response message to store Runway information. 
 * @author Luke
 * @version 8.4
 * @since 8.4
 */

public class RunwayListMessage extends DataResponseMessage<Runway> {
	
	private Airport _aD;
	private Airport _aA;
	
	/**
	 * Instantiates the Message.
	 * @param msgFrom the originating Pilot
	 * @param parentID the request message ID
	 */
	public RunwayListMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, DataRequest.RUNWAYS, parentID);
	}
	
	public Airport getAirportD() {
		return _aD;
	}
	
	public Airport getAirportA() {
		return _aA;
	}
	
	public void setAirportD(Airport a) {
		_aD = a;
	}
	
	public void setAirportA(Airport a) {
		_aA = a;
	}
}