// Copyright 2004, 2005, 2006, 2007, 2008, 2010, 2012, 2014, 2016, 2017, 2018, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import java.util.*;
import java.time.*;

import org.deltava.acars.beans.TXCode;

import org.deltava.beans.*;
import org.deltava.beans.acars.*;
import org.deltava.beans.schedule.*;
import org.deltava.beans.system.OperatingSystem;

import org.deltava.util.StringUtils;

/**
 * An ACARS Flight Information message.
 * @author Luke
 * @version 8.6
 * @since 1.0
 */

public class InfoMessage extends AbstractMessage implements FlightNumber, RoutePair, FlightTimes {
	
	private int _flightID;
	private Instant _startTime;
	private Instant _simStartTime;
	private Instant _simGateTime;
	private Instant _endTime;
	
	private String _eqType;
	private String _livery;
	private String _tailCode;
	
	private Flight _f;
	private Airport _airportA;
	private Airport _airportD;
	private Airport _airportL;
	private String _fpAlt;
	private OnlineNetwork _network;
	private String _comments;
	
	private Simulator _sim = Simulator.UNKNOWN;
	private AutopilotType _ap = AutopilotType.DEFAULT;
	private int _simMajor;
	private int _simMinor;
	private OperatingSystem _os = OperatingSystem.WINDOWS;
	private boolean _isSim64Bit;
	private boolean _isACARS64Bit;
	
	private String _sid;
	private String _star;
	
	private double _loadFactor = -1;
	private int _pax;
	private LoadType _loadType = LoadType.RANDOM;
	
	private boolean _flightComplete;
	private boolean _checkRide;
	private boolean _scheduleValidated;
	private boolean _noRideCheck;
	
	private boolean _dispatchPlan;
	private int _dispatcherID;
	private int _routeID;
	
	private int _txCode = TXCode.DEFAULT_IFR;
	
	private final Collection<String> _waypoints = new LinkedHashSet<String>();
	
	public InfoMessage(Pilot msgFrom) {
		super(MessageType.INFO, msgFrom);
	}
	
	public void addWaypoint(String newWP) {
		_waypoints.add(newWP.toUpperCase());
	}
	
	public int getFlightID() {
		return _flightID;
	}
	
	@Override
	public Airport getAirportA() {
		return _airportA;
	}
	
	@Override
	public Airport getAirportD() {
		return _airportD;
	}
	
	public Airport getAirportL() {
		return _airportL;
	}
	
	@Override
	public Airline getAirline() {
		return _f.getAirline();
	}
	
	@Override
	public int getFlightNumber() {
		return _f.getFlightNumber();
	}
	
	@Override
	public int getLeg() {
		return _f.getLeg();
	}
	
	@Override
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
	
	public String getTailCode() {
		return _tailCode;
	}
	
	public String getFlightCode() {
		return _f.getShortCode();
	}
	
	public double getLoadFactor() {
		return _loadFactor;
	}
	
	public int getPassengers() {
		return _pax;
	}
	
	public LoadType getLoadType() {
		return _loadType;
	}
	
	public OperatingSystem getPlatform() {
		return _os;
	}
	
	public boolean getIsSim64Bit() {
		return _isSim64Bit;
	}
	
	public boolean getIsACARS64Bit() {
		return _isACARS64Bit;
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
	
	public AutopilotType getAutopilotType() {
		return _ap;
	}
	
	public Instant getStartTime() {
		return _startTime;
	}
	
	public Instant getSimStartTime() {
		return _simStartTime;
	}
	
	public Instant getSimGateTime() {
		return _simGateTime;
	}
	
	@Override
	public ZonedDateTime getTimeD() {
		return (_simStartTime == null) ? null : ZonedDateTime.ofInstant(_simStartTime, _airportD.getTZ().getZone());
	}
	
	@Override
	public ZonedDateTime getTimeA() {
		return (_simGateTime == null) ? null : ZonedDateTime.ofInstant(_simGateTime, _airportA.getTZ().getZone());
	}
	
	public Instant getEndTime() {
	   return _endTime;
	}
	
	public int getSimMajor() {
		return _simMajor;
	}
	
	public int getSimMinor() {
		return _simMinor;
	}
	
	public Collection<String> getWaypoints() {
		return _waypoints;
	}
	
	public String getRoute() {
		return StringUtils.listConcat(_waypoints, " ");
	}
	
	public int getTX() {
		return _txCode;
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
	
	public void setTailCode(String code) {
		_tailCode = code;
	}
	
	public void setFlight(Flight f) {
		_f = f;
	}
	
	public void setFlightID(int id) {
		_flightID = id;
	}
	
	public void setSimulator(Simulator sim) {
		_sim = sim;
	}
	
	public void setAutopilotType(AutopilotType ap) {
		_ap = ap;
	}
	
	public void setSimulatorVersion(int major, int minor) {
		_simMajor = Math.max(1, major);
		_simMinor = Math.max(0, minor);
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
	
	public void setStartTime(Instant dt) {
		_startTime = dt;
	}
	
	public void setSimStartTime(Instant dt) {
		_simStartTime = dt;
	}
	
	public void setSimGateTime(Instant dt) {
		_simGateTime = ((dt != null) && dt.isAfter(_simStartTime)) ? dt : _simStartTime;
	}
	
	public void setEndTime(Instant dt) {
	   _endTime = ((dt != null) && dt.isAfter(_startTime)) ? dt : _startTime;
	}
	
	public void setComplete(boolean isComplete) {
		_flightComplete = isComplete;
	}
	
	public void setLoadFactor(double lf) {
		_loadFactor = Math.max(-1, Math.min(1, lf));
	}
	
	public void setPassengers(int pax) {
		_pax = Math.max(-1, pax);
	}
	
	public void setLoadType(LoadType lt) {
		_loadType = lt;
	}
	
	public void setPlatform(OperatingSystem os) {
		_os = os;
	}
	
	public void setIsSim64Bit(boolean is64) {
		_isSim64Bit = is64;
	}
	
	public void setIsACARS64Bit(boolean is64) {
		_isACARS64Bit = is64;
	}
	
	/**
	 * Updates the transponder code.
	 * @param txCode the code
	 */
	public void setTX(int txCode) {
		_txCode = txCode;
	}
	
	public void setWaypoints(String wpList) {
		
		// Remove any non-alphanumeric character with a space
		StringBuilder buf = new StringBuilder();
		for (int x = 0; x < wpList.length(); x++) {
			char c = wpList.charAt(x);
			buf.append(Character.isLetterOrDigit(c) ? c : ' ');
		}
		
		// Split into a tokenizer - replace periods with the waypoint spacer
		StringTokenizer wpTokens = new StringTokenizer(buf.toString(), " ");
		while (wpTokens.hasMoreTokens())
			addWaypoint(wpTokens.nextToken());
	}
}