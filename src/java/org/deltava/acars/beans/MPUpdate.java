// Copyright 2008, 2010, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import org.deltava.beans.DatabaseBean;

import org.deltava.acars.message.LocationMessage;

/**
 * A bean to store multi-player position data, combining location and
 * aircraft model rendering data. 
 * @author Luke
 * @version 4.1
 * @since 2.2
 */

public class MPUpdate extends DatabaseBean {

	private LocationMessage _loc;
	
	private String _callsign;
	private String _aCode;
	private String _eqType;
	private String _livery;
	
	/**
	 * Creates the bean.
	 * @param id the ACARS Flight ID
	 * @param msg the latest location
	 */
	public MPUpdate(int id, LocationMessage msg) {
		super();
		setID(id);
		_loc = msg;
	}

	/**
	 * Returns the aircraft location.
	 * @return the location
	 */
	public LocationMessage getLocation() {
		return _loc;
	}
	
	/**
	 * Returns the Airline code.
	 * @return the airline code
	 */
	public String getAirlineCode() {
		return _aCode;
	}
	
	/**
	 * Returns the aircraft used.
	 * @return the aircraft code
	 */
	public String getEquipmentType() {
		return _eqType;
	}
	
	/**
	 * Returns the livery code.
	 * @return the livery code
	 */
	public String getLiveryCode() {
		return _livery;
	}
	
	/**
	 * Returns the aircraft callsign.
	 * @return the callsign
	 */
	public String getCallsign() {
		return _callsign;
	}
	
	/**
	 * Updates the airline code.
	 * @param code the airline code
	 */
	public void setAirlineCode(String code) {
		_aCode = code;
	}
	
	/**
	 * Updates the livery code for this connection.
	 * @param code the livery code
	 */
	public void setLiveryCode(String code) {
		_livery = code;
	}
	
	/**
	 * Updates the aircraft used.
	 * @param eqType the aircraft code
	 */
	public void setEquipmentType(String eqType) {
		_eqType = eqType;
	}
	
	/**
	 * Updates the aircraft callsign.
	 * @param cs the callsign
	 */
	public void setCallsign(String cs) {
		_callsign = cs;
	}
	
	public String toString() {
		StringBuilder buf = new StringBuilder("MP");
		buf.append(getID());
		buf.append('-');
		buf.append(_loc.getDate().getTime());
		return buf.toString();
	}
	
	public int hashCode() {
		return toString().hashCode();
	}
	
	public boolean equals(Object o) {
		return (o instanceof MPUpdate) ? (hashCode() == o.hashCode()) : false;
	}
}