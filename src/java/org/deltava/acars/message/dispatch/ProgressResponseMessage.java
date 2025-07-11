// Copyright 2007, 2012, 2015, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.dispatch;

import java.util.*;

import org.deltava.beans.*;
import org.deltava.beans.navdata.FIR;
import org.deltava.beans.schedule.*;

import org.deltava.acars.message.DispatchMessage;
import org.deltava.acars.message.DispatchRequest;

/**
 * An ACARS message for Dispatcher progress responses.
 * @author Luke
 * @version 8.4
 * @since 2.1
 */

public class ProgressResponseMessage extends DispatchMessage {
	
	private int _fuel;
	private int _fuelBurn;
	private int _groundSpeed;
	
	private Airport _airportD;
	private Airport _airportA;
	private Airport _airportL;
	private final Collection<Airport> _alternates = new ArrayList<Airport>();
	
	private GeospaceLocation _loc;
	private String _eqType;
	
	private FIR _fir;

	/**
	 * Initializes the Message.
	 * @param msgFrom the originating Dispatcher
	 */
	public ProgressResponseMessage(Pilot msgFrom) {
		super(DispatchRequest.PROGRESS, msgFrom);
	}

	/**
	 * Returns the amount of fuel on the aircraft.
	 * @return the amount of fuel in pounds
	 */
	public int getFuel() {
		return _fuel;
	}
	
	/**
	 * Returns the fuel burn rate.
	 * @return the burn rate in pounds per hour
	 */
	public int getBurnRate() {
		return _fuelBurn;
	}
	
	/**
	 * Returns the aircraft's ground speed.
	 * @return the speed in knots
	 */
	public int getGroundSpeed() {
		return _groundSpeed;
	}
	
	/**
	 * Returns the departure Airport.
	 * @return the Airport
	 */
	public Airport getAirportD() {
		return _airportD;
	}
	
	/**
	 * Returns the destination Airport.
	 * @return the Airport
	 */
	public Airport getAirportA() {
		return _airportA;
	}
	
	/**
	 * Returns the closest Airports suitable for the aircraft.
	 * @return the closest Airports
	 */
	public Collection<Airport> getClosestAirports() {
		return _alternates;
	}
	
	/**
	 * Returns the alternate Airport.
	 * @return the Airport
	 */
	public Airport getAirportL() {
		return _airportL;
	}
	
	/**
	 * Returns the aircraft's current location.
	 * @return the location
	 */
	public GeospaceLocation getLocation() {
		return _loc;
	}
	
	/**
	 * Returns the aircraft type.
	 * @return the aircraft type
	 */
	public String getEquipmentType() {
		return _eqType;
	}
	
	/**
	 * Returns the Flight Information Region for the aircraft position.
	 * @return the FIR, or null if unknown
	 */
	public FIR getFIR() {
		return _fir;
	}
	
	/**
	 * Updates the aircraft's fuel.
	 * @param fuel the amount of fuel in pounds
	 */
	public void setFuel(int fuel) {
		_fuel = Math.max(0, fuel);
	}
	
	/**
	 * Updates the aircraft's fuel burn rate.
	 * @param burnRate the burn rate in pounds per hour
	 */
	public void setBurnRate(int burnRate) {
		_fuelBurn = Math.max(0, burnRate);
	}
	
	/**
	 * Updates the aircraft's ground speed.
	 * @param gs the ground speed in knots
	 */
	public void setGroundSpeed(int gs) {
		_groundSpeed = gs;
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
	 * Updates the alternate Airport.
	 * @param a the Airport
	 */
	public void setAirportL(Airport a) {
		_airportL = a;
	}
	
	/**
	 * Updates the closest suitable Airports.
	 * @param airports a Collection of Airports
	 */
	public void addClosestAirports(Collection<Airport> airports) {
		_alternates.addAll(airports);
	}
	
	/**
	 * Updates the aircraft's current location.
	 * @param loc the location
	 */
	public void setLocation(GeospaceLocation loc) {
		_loc = loc;
	}
	
	/**
	 * Updates the equipment type.
	 * @param eqType the equipment type
	 */
	public void setEquipmentType(String eqType) {
		_eqType = eqType;
	}
	
	/**
	 * Sets the Flight Information Region for the aircraft position.
	 * @param f the FIR
	 */
	public void setFIR(FIR f) {
		_fir = f;
	}
}