package org.deltava.acars.message;

import java.util.*;
import java.text.SimpleDateFormat;

import org.deltava.beans.Pilot;
import org.deltava.beans.schedule.Airport;

/**
 * @author Luke J. Kolin
 */
public class InfoMessage extends AbstractMessage {
	
	// Date/time parser
	private static final SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
	
	// FS Version constants
	public static final int FS_UNKNOWN = 0;
	public static final int FS_2000 = 1;
	public static final int FS_2002 = 2;
	public static final int FS_2004 = 3;
	public static final String[] FS_NAMES = {"???", "FS2000", "FS2002", "FS2004"};
	
	// Bean fields
	private int _flightID;
	private String _eqType;
	private String _flightCode;
	private Airport _airportA;
	private Airport _airportD;
	private Airport _airportL;
	private String _fpAlt;
	private String _comments;
	private int _fsVersion;
	
	private ArrayList _waypoints = new ArrayList();
	
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

	public Airport getAirportL() {
		return _airportL;
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
	
	public void setAirportA(Airport aInfo) {
		_airportA = aInfo;
	}
	
	public void setAirportD(Airport aInfo) {
		_airportD = aInfo;
	}
	
	public void setAirportL(Airport aInfo) {
		_airportL = aInfo;
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
		
		if ((ver >= 0) && (ver < FS_NAMES.length))
			_fsVersion = ver;
	}
	
	public void setFSVersion(String txtVer) {
		
		for (int x = 0; x < FS_NAMES.length; x++) {
			if (FS_NAMES[x].equals(txtVer)) {
				_fsVersion = x;
				break;
			}
		}
	}

	public void setWaypoints(String wpList) {
		
		// Split into a tokenizer
		StringTokenizer wpTokens = new StringTokenizer(wpList, WAYPOINT_SPACER);
		while (wpTokens.hasMoreTokens())
			addWaypoint(wpTokens.nextToken());
	}
}