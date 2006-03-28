// Copyright 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import java.util.*;

import org.deltava.beans.Pilot;
import org.deltava.beans.schedule.Airport;

import org.deltava.util.StringUtils;

/**
 * An ACARS message to store dispatch information.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class DispatchMessage extends AbstractMessage {
	
	private String _recipient;
	private String _flightCode;
	private int _leg;
	
	private String _eqType;
	private Collection<String> _route = new LinkedHashSet<String>();
	private Airport _airportD;
	private Airport _airportA;
	private Airport _airportL;
	
	private int _txCode;
	private int _fuel;

	/**
	 * Creates a new Dispatch data message.
	 * @param msgFrom the originator
	 * @param recipient the recipient
	 */
	public DispatchMessage(Pilot msgFrom, String recipient) {
		super(Message.MSG_DISPATCH, msgFrom);
		_recipient = recipient;
	}

	public String getFlightCode() {
		return _flightCode;
	}
	
	public int getLeg() {
		return _leg;
	}
	
	public String getEquipmentType() {
		return _eqType;
	}
	
	public String getRecipient() {
		return _recipient;
	}
	
	public String getRoute() {
		return StringUtils.listConcat(_route, " ");
	}
	
	public Airport getAirportD() {
		return _airportD;
	}
	
	public Airport getAirportA() {
		return _airportA;
	}
	
	public Airport getAirportL() {
		return _airportL;
	}
	
	public int getFuel() {
		return _fuel;
	}
	
	public int getTXCode() {
		return _txCode;
	}
	
	public void addWaypoint(String waypoint) {
		_route.add(waypoint.toUpperCase());
	}
	
	public void setEquipmentType(String eqType) {
		_eqType = eqType;
	}
	
	public void setFlightCode(String fCode) {
		_flightCode = fCode.toUpperCase();
	}
	
	public void setAirportD(Airport a) {
		_airportD = a;
	}
	
	public void setAirportA(Airport a) {
		_airportA = a;
	}
	
	public void setAirportL(Airport a) {
		_airportL = a;
	}
	
	public void setTXCode(int sqCode) {
		_txCode = (sqCode < 1000) ? 2200 : sqCode;
	}
	
	public void setFuel(int fuel) {
		_fuel = (fuel < 1) ? 1 : _fuel;
	}
	
	public void setLeg(int leg) {
		_leg = ((leg < 1) || (leg > 5)) ? 1 : leg;
	}
}