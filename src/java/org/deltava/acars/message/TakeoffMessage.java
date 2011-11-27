// Copyright 2009, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.*;
import org.deltava.beans.flight.ILSCategory;
import org.deltava.beans.schedule.Airport;

/**
 * An ACARS message to track takeoffs and landings.
 * @author Luke
 * @version 4.1
 * @since 2.8
 */

public class TakeoffMessage extends AbstractMessage {
	
	private GeospaceLocation _loc;
	private int _hdg;
	
	private boolean _isTakeoff;
	private ILSCategory _ils;
	
	private String _flightCode;
	private String _eqType;
	private Airport _airportD;
	private Airport _airportA;

	/**
	 * Creates a new Message.
	 * @param msgFrom the originating Pilot
	 */
	public TakeoffMessage(Pilot msgFrom) {
		super(Message.MSG_TOTD, msgFrom);
		setProtocolVersion(2);
	}
	
	/**
	 * Returns the takeoff/landing location.
	 * @return the location
	 */
	public GeospaceLocation getLocation() {
		return _loc;
	}
	
	/**
	 * Returns the aircraft heading at takeoff/landing.
	 * @return the heading in degrees
	 */
	public int getHeading() {
		return _hdg;
	}
	
	/**
	 * Returns the departure Airport.
	 * @return the Airport
	 */
	public Airport getAirportD() {
		return _airportD;
	}
	
	/**
	 * Returns the arrival Airport
	 * @return the Airport
	 */
	public Airport getAirportA() {
		return _airportA;
	}
	
	/**
	 * Returns the ILS category.
	 * @return the ILS Category, or null if a takeoff
	 */
	public ILSCategory getILS() {
		return _ils;
	}
	
	/**
	 * Returns whether this is a takeoff or landing.
	 * @return TRUE if takeoff, otherwise FALSE
	 */
	public boolean isTakeoff() {
		return _isTakeoff;
	}
	
	/**
	 * Returns the Flight code.
	 * @return the flight code
	 */
	public String getFlightCode() {
		return _flightCode;
	}
	
	/**
	 * Returns the equipment type.
	 * @return the equipment type
	 */
	public String getEquipmentType() {
		return _eqType;
	}
	
	/**
	 * Updates the takeoff/touchdown location.
	 * @param loc the location
	 */
	public void setLocation(GeospaceLocation loc) {
		_loc = loc;
	}
	
	/**
	 * Updates the takeoff/touchdown heading.
	 * @param hdg the heading in degrees
	 */
	public void setHeading(int hdg) {
		_hdg = hdg;
	}
	
	/**
	 * Sets wheter this is a takeoff or a landing.
	 * @param isTakeoff TRUE if takeoff, otherwise FALSE
	 */
	public void setTakeoff(boolean isTakeoff) {
		_isTakeoff = isTakeoff;
	}
	
	/**
	 * Sets the ILS category.
	 * @param ilscat the ILS category
	 */
	public void setILS(ILSCategory ilscat) {
		_ils = ilscat;
	}
	
	/**
	 * Updates the Flight code.
	 * @param code the flight code
	 */
	public void setFlightCode(String code) {
		_flightCode = code;
	}
	
	/**
	 * Updates the equipment type.
	 * @param eqType the equipment type
	 */
	public void setEquipmentType(String eqType) {
		_eqType = eqType;
	}
	
	/**
	 * Updates the departure Airport.
	 * @param a the Airport
	 */
	public void setAirportD(Airport a) {
		_airportD = a;
	}
	
	/**
	 * Updates the arrival Airport.
	 * @param a the Airport
	 */
	public void setAirportA(Airport a) {
		_airportA = a;
	}
}