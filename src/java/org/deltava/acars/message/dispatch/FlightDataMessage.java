// Copyright 2006, 2007, 2008, 2010, 2011, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.dispatch;

import java.util.*;

import org.deltava.beans.*;
import org.deltava.beans.navdata.*;
import org.deltava.beans.schedule.*;

import org.deltava.acars.message.*;

/**
 * An ACARS message to store Dispatch data.
 * @author Luke
 * @version 5.1
 * @since 1.1
 */

public class FlightDataMessage extends DispatchMessage {
	
	private String _recipient;
	private int _routeID;
	private Airline _a;
	private int _flight;
	private int _leg;
	
	private Simulator _sim = Simulator.UNKNOWN;
	
	private boolean _routeValid;
	private boolean _noSave;
	
	private String _eqType;
	private String _cruiseAlt;
	private Gate _gateD;
	private Gate _gateA;
	
	private Airport _airportD;
	private Airport _airportA;
	private Airport _airportL;
	
	private final Collection<NavigationDataBean> _route = new LinkedHashSet<NavigationDataBean>();
	private String _routeText;
	
	private String _sid;
	private String _star;
	
	private String _comments;
	
	private int _txCode;
	private final Map<FuelTank, Integer> _fuelAmount = new LinkedHashMap<FuelTank, Integer>();
	
	/**
	 * Creates a new Dispatch data message.
	 * @param msgFrom the originator
	 */
	public FlightDataMessage(Pilot msgFrom) {
		super(DispatchMessage.DSP_INFO, msgFrom);
	}
	
	public Airline getAirline() {
		return _a;
	}

	public int getFlight() {
		return _flight;
	}
	
	public int getLeg() {
		return _leg;
	}
	
	public Simulator getSimulator() {
		return _sim;
	}
	
	public String getCruiseAltitude() {
		return _cruiseAlt;
	}
	
	public String getComments() {
		return _comments;
	}
	
	public int getRouteID() {
		return _routeID;
	}
	
	public String getEquipmentType() {
		return _eqType;
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
	
	public Gate getGateD() {
		return _gateD;
	}
	
	public Gate getGateA() {
		return _gateA;
	}
	
	public String getSID() {
		return _sid;	
	}
	
	public String getSTAR() {
		return _star;
	}
	
	public boolean getNoSave() {
		return _noSave;
	}
	
	public boolean isRouteValid() {
		return _routeValid;
	}
	
	public Map<FuelTank, Integer> getFuel() {
		return new LinkedHashMap<FuelTank, Integer>(_fuelAmount);
	}
	
	public int getTXCode() {
		return _txCode;
	}
	
	public Collection<NavigationDataBean> getWaypoints() {
		return _route;
	}
	
	public String getRoute() {
		return _routeText;
	}
	
	public void addWaypoint(NavigationDataBean wp) {
		_route.add(wp);
	}
	
	public void setRouteID(int id) {
		_routeID = id;
	}
	
	public void setRoute(String route) {
		_routeText = route;
	}
	
	public void setEquipmentType(String eqType) {
		_eqType = eqType;
	}
	
	public void setFlight(int flight) {
		_flight = Math.max(1, flight);
	}
	
	public void setAirline(Airline a) {
		_a = a;
	}
	
	public void setSimulator(Simulator sim) {
		_sim = sim;
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
	
	public void setGateD(Gate g) {
		_gateD = g;
	}
	
	public void setGateA(Gate g) {
		_gateA = g;
	}
	
	public void setSID(String sid) {
		_sid = sid;
	}
	
	public void setSTAR(String star) {
		_star = star;
	}
	
	public void setCruiseAltitude(String alt) {
		_cruiseAlt = alt;
	}
	
	public void setTXCode(int sqCode) {
		_txCode = (sqCode < 1000) ? 2200 : sqCode;
	}
	
	public void addFuel(FuelTank tank, int fuel) {
		if (fuel > 0)
			_fuelAmount.put(tank, Integer.valueOf(fuel));
	}
	
	public void setLeg(int leg) {
		_leg = Math.min(5, Math.max(1, leg));
	}
	
	public void setRouteValid(boolean isValid) {
		_routeValid = isValid;
	}
	
	public void setNoSave(boolean noSave) {
		_noSave = (_recipient != null) && noSave;
	}
	
	public void setComments(String comments) {
		_comments = comments;
	}
}