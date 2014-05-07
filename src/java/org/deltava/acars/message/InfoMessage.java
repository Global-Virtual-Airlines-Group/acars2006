// Copyright 2004, 2005, 2006, 2007, 2008, 2010, 2012, 2014 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import java.util.*;

import org.deltava.beans.*;
import org.deltava.beans.schedule.Airport;
import org.deltava.beans.schedule.RoutePair;

/**
 * An ACARS Flight Information message.
 * @author Luke
 * @version 5.4
 * @since 1.0
 */

public class InfoMessage extends AbstractMessage implements RoutePair {
	
	private int _flightID;
	private Date _startTime;
	private Date _endTime;
	
	private String _eqType;
	private String _livery;
	
	private String _flightCode;
	private Airport _airportA;
	private Airport _airportD;
	private Airport _airportL;
	private String _fpAlt;
	private OnlineNetwork _network;
	private String _comments;
	private Simulator _sim = Simulator.UNKNOWN;
	
	private String _sid;
	private String _star;
	
	private boolean _flightComplete;
	private boolean _checkRide;
	private boolean _scheduleValidated;
	private boolean _noRideCheck;
	
	private boolean _dispatchPlan;
	private int _dispatcherID;
	private int _routeID;
	
	private final Collection<String> _waypoints = new LinkedHashSet<String>();
	
	public InfoMessage(Pilot msgFrom) {
		super(Message.MSG_INFO, msgFrom);
	}
	
	public void addWaypoint(String newWP) {
		_waypoints.add(newWP.toUpperCase());
	}
	
	public int getFlightID() {
		return _flightID;
	}
	
	public Airport getAirportA() {
		return _airportA;
	}
	
	public Airport getAirportD() {
		return _airportD;
	}
	
	public Airport getAirportL() {
		return _airportL;
	}
	
	public int getDistance() {
		return _airportD.getPosition().distanceTo(_airportA);
	}

	public String getAltitude() {
		return _fpAlt;
	}
	
	public String getComments() {
		return _comments;
	}
	
	public OnlineNetwork getNetwork() {
		return _network;
	}
	
	public String getEquipmentType() {
		return _eqType;
	}
	
	public String getLivery() {
		return _livery;
	}
	
	public String getFlightCode() {
		return _flightCode;
	}
	
	public String getSID() {
		return _sid;
	}
	
	public String getSTAR() {
		return _star;
	}
	
	public Simulator getSimulator() {
		return _sim;
	}
	
	public Date getStartTime() {
		return _startTime;
	}
	
	public Date getEndTime() {
	   return _endTime;
	}
	
	public Collection<String> getWaypoints() {
		return _waypoints;
	}
	
	public String getRoute() {
		StringBuilder buf = new StringBuilder();
		for (Iterator<String> i = _waypoints.iterator(); i.hasNext(); ) {
			buf.append(i.next());
			if (i.hasNext())
				buf.append(' ');
		}
		
		// Return the buffer
		return buf.toString();
	}
	
	public boolean isComplete() {
		return _flightComplete;
	}
	
	public boolean isCheckRide() {
		return _checkRide;
	}
	
	public boolean isDispatchPlan() {
		return _dispatchPlan;
	}
	
	public int getDispatcherID() {
		return _dispatcherID;
	}
	
	public int getRouteID() {
		return _routeID;
	}
	
	public boolean isScheduleValidated() {
		return _scheduleValidated;
	}
	
	public boolean isNoRideCheck() {
		return _noRideCheck;
	}
	
	public boolean matches(Airport org, Airport dst) {
		return (_airportD.equals(org) && _airportA.equals(dst));
	}
	
	public void setAirportA(Airport aInfo) {
		_airportA = aInfo;
	}
	
	public void setAirportD(Airport aInfo) {
		_airportD = aInfo;
	}
	
	public void setAirportL(Airport aInfo) {
		_airportL = aInfo;
	}
	
	public void setSID(String code) {
		_sid = code;
	}
	
	public void setSTAR(String code) {
		_star = code;
	}
	
	public void setAltitude(String newFPAlt) {
		_fpAlt = newFPAlt;
	}
	
	public void setComments(String newComments) {
		_comments = newComments;
	}
	
	public void setNetwork(OnlineNetwork network) {
		_network = network;
	}
	
	public void setEquipmentType(String newEQ) {
		_eqType = newEQ;
	}
	
	public void setLivery(String code) {
		_livery = code;
	}
	
	public void setFlightCode(String newCode) {
		_flightCode = newCode;
	}
	
	public void setFlightID(int id) {
		_flightID = id;
	}
	
	public void setSimulator(Simulator sim) {
		_sim = sim;
	}
	
	public void setCheckRide(boolean isCR) {
		_checkRide = isCR;
	}
	
	public void setNoRideCheck(boolean noCheck) {
		_noRideCheck = noCheck;
	}
	
	public void setScheduleValidated(boolean isOK) {
		_scheduleValidated = isOK;
	}
	
	public void setDispatchPlan(boolean isDP) {
		_dispatchPlan = isDP;
	}
	
	public void setDispatcherID(int id) {
		_dispatcherID = Math.max(0, id);
	}
	
	public void setRouteID(int id) {
		_routeID = Math.max(0, id);
	}
	
	public void setStartTime(Date dt) {
		_startTime = dt;
	}
	
	public void setEndTime(Date dt) {
	   _endTime = ((dt != null) && (dt.after(_startTime))) ? dt : _startTime;
	}
	
	public void setComplete(boolean isComplete) {
		_flightComplete = isComplete;
	}
	
	public void setWaypoints(String wpList) {
		
		// Remove any non-alphanumeric character with a space
		StringBuilder buf = new StringBuilder();
		for (int x = 0; x < wpList.length(); x++) {
			char c = wpList.charAt(x);
			if (Character.isLetterOrDigit(c))
				buf.append(c);
			else
				buf.append(' ');
		}
		
		// Split into a tokenizer - replace periods with the waypoint spacer
		StringTokenizer wpTokens = new StringTokenizer(buf.toString(), " ");
		while (wpTokens.hasMoreTokens())
			addWaypoint(wpTokens.nextToken());
	}
}