// Copyright 2009, 2011, 2018, 2019, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.*;
import org.deltava.beans.flight.ILSCategory;
import org.deltava.beans.schedule.*;

/**
 * An ACARS message to track takeoffs and landings.
 * @author Luke
 * @version 11.0
 * @since 2.8
 */

public class TakeoffMessage extends AbstractMessage implements RoutePair, GeoLocation {
	
	private final GeoLocation _loc;
	private int _hdg;
	private int _vSpeed;
	
	private boolean _isTakeoff;
	private ILSCategory _ils;
	private double _score;
	
	private String _flightCode;
	private String _eqType;
	private Airport _airportD;
	private Airport _airportA;

	/**
	 * Creates a new Message.
	 * @param msgFrom the originating Pilot
	 * @param loc the takeoff/touchdown location
	 */
	public TakeoffMessage(Pilot msgFrom, GeoLocation loc) {
		super(MessageType.TOTD, msgFrom);
		setProtocolVersion(2);
		_loc = loc;
	}
	
	/**
	 * Returns the aircraft heading at takeoff/landing.
	 * @return the heading in degrees
	 */
	public int getHeading() {
		return _hdg;
	}
	
	/**
	 * Returns the vertical speed at takeoff/touchdown.
	 * @return the speed in feet/minute
	 */
	public int getVSpeed() {
		return _vSpeed;
	}
	
	@Override
	public Airport getAirportD() {
		return _airportD;
	}
	
	@Override
	public Airport getAirportA() {
		return _airportA;
	}

	@Override
	public double getLatitude() {
		return _loc.getLatitude();
	}

	@Override
	public double getLongitude() {
		return _loc.getLongitude();
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
	 * Returns the landing score.
	 * @return the score
	 */
	public double getScore() {
		return _score;
	}
	
	/**
	 * Updates the takeoff/touchdown heading.
	 * @param hdg the heading in degrees
	 */
	public void setHeading(int hdg) {
		_hdg = hdg;
	}
	
	/**
	 * Updates the vertical speed at takeoff/touchdown.
	 * @param vs the speed in feet/minute
	 */
	public void setVSpeed(int vs) {
		_vSpeed = vs;
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
	
	/**
	 * Updates the landing score.
	 * @param score the score
	 */
	public void setScore(double score) {
		_score = score;
	}
}