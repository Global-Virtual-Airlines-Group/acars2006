// Copyright 2007, 2008, 2010, 2011, 2012, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.dispatch;

import java.util.*;

import org.deltava.beans.*;
import org.deltava.beans.navdata.Gate;
import org.deltava.beans.schedule.*; 

import org.deltava.util.GeoUtils;

import org.deltava.acars.message.DispatchMessage;

/**
 * An ACARS message to transmit dispatch requests.
 * @author Luke
 * @version 7.2
 * @since 2.0
 */

public class RequestMessage extends DispatchMessage implements GeoLocation, RoutePair {
	
	private Airline _a;
	private Airport _airportD;
	private Airport _airportA;
	private Airport _airportL;
	
	private Gate _gateD;
	private final Collection<Gate> _arrivalGates = new LinkedHashSet<Gate>();
	
	private GeoLocation _loc = new GeoPosition(0, 0);
	
	private String _eqType;
	private Simulator _sim;
	private int _maxGrossWeight;
	private int _zeroFuelWeight;
	private boolean _routeValid;
	private boolean _autoDispatch;
	private boolean _etopsWarn;
	
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
	@Override
	public Airport getAirportD() {
		return _airportD;
	}
	
	/**
	 * Returns the arrival airport for this flight.
	 * @return the Airport
	 */
	@Override
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
	 * Returns the requesting pilot's latitude.
	 * @return the latitude in degrees
	 */
	@Override
	public double getLatitude() {
		return _loc.getLatitude();
	}
	
	/**
	 * Returns the requesting pilot's longitude.
	 * @return the longitude in degrees
	 */
	@Override
	public double getLongitude() {
		return _loc.getLongitude();
	}
	
	/**
	 * Returns the equipment type used by the Pilot.
	 * @return the equipment type
	 */
	public String getEquipmentType() {
		return _eqType;
	}
	
	/**
	 * Returns the pilot's simulator.
	 * @return a Simulator
	 */
	public Simulator getSimulator() {
		return _sim;
	}
	
	/**
	 * Returns the closest gate to the Aircraft's position.
	 * @return the closest Gate, or null
	 */
	public Gate getClosestGate() {
		return _gateD;
	}

	/**
	 * Returns the available arrival Gates.
	 * @return a Collection of Gates
	 */
	public Collection<Gate> getArrivalGates() {
		return _arrivalGates;
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
	 * Returns whether the route has an ETOPS warning.
	 * @return TRUE if an ETOPS warning, otherwise FALSE
	 */
	public boolean getETOPSWarning() {
		return _etopsWarn;
	}
	
	@Override
	public int getDistance() {
		return GeoUtils.distance(_airportD, _airportA);
	}
	
	/**
	 * Returns if automatic dispatch services requested.
	 * @return TRUE if auto-dispatch, otherwise FALSE
	 * @see RequestMessage#setAutoDispatch(boolean)
	 */
	public boolean isAutoDispatch() {
		return _autoDispatch;
	}

	/**
	 * Returns whether the requested route is valid.
	 * @return TRUE if the route is valid, otherwise FALSE
	 */
	public boolean isRouteValid() {
		return _routeValid;
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
			_tankSizes.put(tank, Integer.valueOf(size));
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
	 * Updates the pilot's Simulator
	 * @param sim a Simulator
	 */
	public void setSimulator(Simulator sim) {
		_sim = sim;
	}
	
	/**
	 * Updates the location of the pilot.
	 * @param loc the requesting Pilot's location
	 */
	public void setLocation(GeoLocation loc) {
		_loc = new GeoPosition(loc);
	}
	
	/**
	 * Enables auto-dispatch mode, where human dispatcher intervention is
	 * skipped if routes already exist in the database.
	 * @param isAuto TRUE if auto-dispatch mode, otherwise FALSE
	 */
	public void setAutoDispatch(boolean isAuto) {
		_autoDispatch = isAuto;
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
	
	/**
	 * Updates the closest Gate to the aircraft's position.
	 * @param g a Gate
	 */
	public void setClosestGate(Gate g) {
		_gateD = g;
	}
	
	/**
	 * Updates the list of arrival Gates.
	 * @param gates a Collection of Gates
	 */
	public void setArrivalGates(Collection<Gate> gates) {
		_arrivalGates.addAll(gates);
	}
	
	/**
	 * Updates whether this is a valid route.
	 * @param isValid TRUE if the route is valid, otherwise FALSE
	 */
	public void setRouteValid(boolean isValid) {
		_routeValid = isValid;
	}

	/**
	 * Updates whether there is an ETOPS warning for this route.
	 * @param isWarn TRUE if there is an ETOPS warning, otherwise FALSE
	 */
	public void setETOPSWarning(boolean isWarn) {
		_etopsWarn = isWarn;
	}
}