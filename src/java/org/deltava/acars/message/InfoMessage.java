package org.deltava.acars.message;

import java.util.*;

import org.deltava.beans.Pilot;
import org.deltava.beans.schedule.Airport;

/**
 * @author Luke J. Kolin
 * @version 1.0
 * @since 1.0
 */

public class InfoMessage extends AbstractMessage {
	
	// FSUIPC Flight Simulator version constants - 1002/1001 are CFS2/CFS1
	private int[] FSUIPC_FS_VERSIONS = {95, 98, 2000, 1002, 1001, 2002, 2004};
	
	// Bean fields
	private int _flightID;
	private String _eqType;
	private String _flightCode;
	private Airport _airportA;
	private Airport _airportD;
	private String _fpAlt;
	private String _comments;
	private int _fsVersion;
	
	private boolean _offlineFlight;
	private boolean _flightComplete;
	
	private ArrayList _waypoints = new ArrayList();
	private Set _offlinePositions;
	
	// Constant for splitting waypoint lists
	private final static String WAYPOINT_SPACER = " ";

	public InfoMessage(Pilot msgFrom) {
		super(Message.MSG_INFO, msgFrom);
	}
	
	public void addWaypoint(String newWP) {
		newWP = newWP.toUpperCase();
		if (!_waypoints.contains(newWP))		
			_waypoints.add(newWP);
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
	
	public Collection getWaypoints() {
		return _waypoints;
	}
	
	public String getAllWaypoints(char sep) {
		
		StringBuffer buf = new StringBuffer();
		for (Iterator i = _waypoints.iterator(); i.hasNext(); ) {
			buf.append((String) i.next());
			if (i.hasNext())
				buf.append(sep);
		}
		
		// Return the buffer
		return buf.toString();
	}
	
	public String getAllWaypoints() {
		return getAllWaypoints(WAYPOINT_SPACER.charAt(0));
	}
	
	public Collection getPositions() {
	   return _offlinePositions;
	}
	
	public boolean isComplete() {
		return _flightComplete;
	}
	
	public boolean isOffline() {
		return _offlineFlight;
	}
	
	public void addPosition(PositionMessage pmsg) {
	   if (_offlineFlight)
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
		} else if ((ver >= 0) && (ver < FSUIPC_FS_VERSIONS.length)) {
			_fsVersion = FSUIPC_FS_VERSIONS[ver];
		} else {
			_fsVersion = 2004;
		}
	}
	
	public void setOffline(boolean isOffline) {
		_offlineFlight = isOffline;
		if (_offlinePositions == null)
		   _offlinePositions = new TreeSet();
	}
	
	public void setComplete(boolean isComplete) {
		_flightComplete = isComplete;
	}
	
	public void setWaypoints(String wpList) {
		
		// Split into a tokenizer
		StringTokenizer wpTokens = new StringTokenizer(wpList, WAYPOINT_SPACER);
		while (wpTokens.hasMoreTokens())
			addWaypoint(wpTokens.nextToken());
	}
}