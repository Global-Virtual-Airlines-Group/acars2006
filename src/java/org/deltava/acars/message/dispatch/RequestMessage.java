// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.dispatch;

import java.util.*;

import org.deltava.beans.Pilot;
import org.deltava.beans.schedule.*; 

import org.deltava.acars.beans.FuelTank;
import org.deltava.acars.message.DispatchMessage;

/**
 * An ACARS message to transmit dispatch requests.
 * @author Luke
 * @version 2.0
 * @since 2.0
 */

public class RequestMessage extends DispatchMessage {
	
	private Airline _a;
	private Airport _airportD;
	private Airport _airportA;
	private Airport _airportL;
	
	private String _eqType;
	private int _maxGrossWeight;
	private int _zeroFuelWeight;
	
	private final Map<FuelTank, Integer> _tankSizes = new TreeMap<FuelTank, Integer>();

	/**
	 * Creates a new dispatch request message.
	 * @param msgFrom the originating Pilot
	 */
	public RequestMessage(Pilot msgFrom) {
		super(DSP_SVCREQ, msgFrom);
	}
	
	/**
	 * Returns the airline for this flight.
	 * @return the Airline
	 */
	public Airline getAirline() {
		return _a;
	}
	
	/**
	 * Returns the departure airport for this flight.
	 * @return the Airport
	 */
	public Airport getAirportD() {
		return _airportD;
	}
	
	/**
	 * Returns the arrival airport for this flight.
	 * @return the Airport
	 */
	public Airport getAirportA() {
		return _airportA;
	}
	
	/**
	 * Returns the alternate airport for this flight.
	 * @return the Airport, or null if none
	 */
	public Airport getAirportL() {
		return _airportL;
	}
	
	/**
	 * Returns the equipment type used by the Pilot.
	 * @return the equipment type
	 */
	public String getEquipmentType() {
		return _eqType;
	}
	
	/**
	 * Returns the maximum weight of the aircraft.
	 * @return the maximum gross weight in pounds
	 */
	public int getMaxWeight() {
		return _maxGrossWeight;
	}
	
	/**
	 * Returns the empty weight of the aircraft.
	 * @return the zero fuel weight in pounds
	 */
	public int getZeroFuelWeight() {
		return _zeroFuelWeight;
	}
	
	/**
	 * Returns the sizes of the aircraft's fuel tanks.
	 * @return Map of tank sizes, keyed by FuelTank
	 */
	public Map<FuelTank, Integer> getTankSizes() {
		return new LinkedHashMap<FuelTank, Integer>(_tankSizes);
	}
	
	/**
	 * Adds a fuel tank to this message.
	 * @param tank the FuelTank code
	 * @param size the tank size in pounds
	 */
	public void addTank(FuelTank tank, int size) {
		if (size > 0)
			_tankSizes.put(tank, new Integer(size));
	}
	
	/**
	 * Sets the maximum gross weight of the aircraft.
	 * @param weight the maximum weight in pounds
	 */
	public void setMaxWeight(int weight) {
		_maxGrossWeight = Math.max(1, weight);
	}
	
	/**
	 * Sets the empty weight of the aircraft.
	 * @param weight the zero fuel weight in pounds
	 */
	public void setZeroFuelWeight(int weight) {
		_zeroFuelWeight = Math.max(1, weight);
	}
	
	/**
	 * Updates the equipment used on this flight.
	 * @param eqType the equipment code
	 */
	public void setEquipmentType(String eqType) {
		_eqType = eqType;
	}
	
	/**
	 * Updates the airline used on this flight.
	 * @param a the Airline
	 */
	public void setAirline(Airline a) {
		_a = a;
	}
	
	/**
	 * Updates the departure Airport on this flight.
	 * @param a the Airport
	 */
	public void setAirportD(Airport a) {
		_airportD = a;
	}
	
	/**
	 * Updates the arrival Airport on this flight.
	 * @param a the Airport
	 */
	public void setAirportA(Airport a) {
		_airportA = a;
	}
	
	/**
	 * Updates the alternate Airport on this flight.
	 * @param a the Airport
	 */
	public void setAirportL(Airport a) {
		_airportL = a;
	}
}