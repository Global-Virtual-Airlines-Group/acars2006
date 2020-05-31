// Copyright 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

/**
 * A class to filter ACARS Connections.
 * @author Luke
 * @version 9.0
 * @since 9.0
 */

public class ConnectionFilter {

	private final String _airlineCode;
	private final long _skipID;
	
	private boolean _includeHidden;
	private boolean _crossAirline;
	private boolean _dispatchOnly;
	
	/**
	 * Creates the filter.
	 * @param airlineCode the airline code to include
	 * @param skipID a Connection ID to skip
	 */
	public ConnectionFilter(String airlineCode, long skipID) {
		super();
		_airlineCode = airlineCode;
		_skipID = skipID;
	}
	
	/**
	 * Sets whether to include hidden connections. 
	 * @param includeHidden TRUE if hidden connections are included, otherwise FALSE
	 */
	public void setIncludeHidden(boolean includeHidden) {
		_includeHidden = includeHidden;
	}
	
	/**
	 * Sets whether to include connections aross virtual airlines. 
	 * @param crossAirline TRUE if connections in other virtual airlines are included, otherwise FALSE
	 */
	public void setCrossAirline(boolean crossAirline) {
		_crossAirline = crossAirline;
	}
	
	/**
	 * Sets whether to only select dispatcher connections.
	 * @param isDispatch TRUE if only dispatch connections are selected, otherwise FALSE
	 */
	public void setDispatchOnly(boolean isDispatch) {
		_dispatchOnly = isDispatch;
	}
	
	/**
	 * Filters an ACARS connection based on criteira.
	 * @param ac an ACARSConnection bean
	 * @return TRUE to include, otherwise FALSE
	 */
	public boolean filter(ACARSConnection ac) {
		
		// Auth user and hidden connections
		if (!ac.isAuthenticated() || (ac.getID() == _skipID)) return false;
		if (ac.getUserHidden() && !_includeHidden) return false;
		
		// Check for dispatch
		if (!ac.getIsDispatch() && _dispatchOnly) return false;
		
		// Airline match
		String airlineCode = ac.getUserData().getAirlineCode();
		return _airlineCode.equals(airlineCode) || _crossAirline;
	}
}