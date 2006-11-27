// Copyright 2004, 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import java.util.*;

import org.deltava.beans.Pilot;
import org.deltava.beans.schedule.Airport;

/**
 * An ACARS Flight Information message.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class InfoMessage extends AbstractMessage {
	
	// FSUIPC Flight Simulator version constants - 1002/1001 are CFS2/CFS1
	private int[] FSUIPC_FS_VERSIONS = {95, 98, 2000, 1002, 1001, 2002, 2004, 2006};
	
	// Bean fields
	private int _flightID;
	private Date _startTime;
	private Date _endTime;
	
	private String _eqType;
	private String _flightCode;
	private Airport _airportA;
	private Airport _airportD;
	private String _fpAlt;
	private String _comments;
	private int _fsVersion;
	
	private boolean _offlineFlight;
	private boolean _flightComplete;
	private boolean _checkRide;
	private boolean _scheduleValidated;
	
	private final Collection<String> _waypoints = new LinkedHashSet<String>();
	private final Set<PositionMessage> _offlinePositions = new TreeSet<PositionMessage>();
	
	// Constant for splitting waypoint lists
	private final static String WAYPOINT_SPACER = " ";

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

	public String getAltitude() {
		return _fpAlt;
	}
	
	public String getComments() {
		return _comments;
	}
	
	public String getEquipmentType() {
		return _eqType;
	}
	
	public String getFlightCode() {
		return _flightCode;
	}
	
	public int getFSVersion() {
		return _fsVersion;
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
	
	public String getAllWaypoints(char sep) {
		
		StringBuilder buf = new StringBuilder();
		for (Iterator<String> i = _waypoints.iterator(); i.hasNext(); ) {
			buf.append(i.next());
			if (i.hasNext())
				buf.append(sep);
		}
		
		// Return the buffer
		return buf.toString();
	}
	
	public String getAllWaypoints() {
		return getAllWaypoints(WAYPOINT_SPACER.charAt(0));
	}
	
	public synchronized Collection<PositionMessage> getPositions() {
	   return _offlinePositions;
	}
	
	public boolean isComplete() {
		return _flightComplete;
	}
	
	public boolean isOffline() {
		return _offlineFlight;
	}
	
	public boolean isCheckRide() {
		return _checkRide;
	}
	
	public boolean isScheduleValidated() {
		return _scheduleValidated;
	}
	
	public synchronized void addPosition(PositionMessage pmsg) {
		_offlinePositions.add(pmsg);
	}
	
	public void setAirportA(Airport aInfo) {
		_airportA = aInfo;
	}
	
	public void setAirportD(Airport aInfo) {
		_airportD = aInfo;
	}
	
	public void setAltitude(String newFPAlt) {
		_fpAlt = newFPAlt;
	}
	
	public void setComments(String newComments) {
		_comments = newComments;
	}
	
	public void setEquipmentType(String newEQ) {
		_eqType = newEQ;
	}
	
	public void setFlightCode(String newCode) {
		_flightCode = newCode;
	}
	
	public void setFlightID(int id) {
		_flightID = id;
	}
	
	public void setFSVersion(int ver) {
		if (ver > 20) {
			_fsVersion = ver;
		} else if ((ver > 0) && (ver < FSUIPC_FS_VERSIONS.length)) {
			_fsVersion = FSUIPC_FS_VERSIONS[ver - 1];
		} else {
			_fsVersion = 2004;
		}
	}
	
	public void setOffline(boolean isOffline) {
		_offlineFlight = isOffline;
	}
	
	public void setCheckRide(boolean isCR) {
		_checkRide = isCR;
	}
	
	public void setScheduleValidated(boolean isOK) {
		_scheduleValidated = isOK;
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
		
		// Split into a tokenizer - replace periods with the waypoint spacer
		StringTokenizer wpTokens = new StringTokenizer(wpList.replaceAll("[.]+", WAYPOINT_SPACER), WAYPOINT_SPACER);
		while (wpTokens.hasMoreTokens())
			addWaypoint(wpTokens.nextToken());
	}
}