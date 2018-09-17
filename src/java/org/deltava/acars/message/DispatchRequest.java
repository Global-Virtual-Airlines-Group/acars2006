// Copyright 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

/**
 * An enumeration of Dispatch operation types. 
 * @author Luke
 * @version 8.4
 * @since 8.4
 */

public enum DispatchRequest {
	UNKNOWN("?"), SVCREQ("svcrequest"), CANCEL("cancel"), ACCEPT("accept"), INFO("flightinfo"), ROUTEREQ("routerequest"),
	ROUTEDATA("routes"), COMPLETE("done"), PROGRESS("progress"), RANGE("svcrange"), SCOPEINFO("scopeinfo"), ROUTEPLOT("rtplot");
	
	private final String _code;
	
	/**
	 * Creates the Dispatch operation.
	 * @param code the operation code
	 */
	DispatchRequest(String code) {
		_code = code;
	}

	/**
	 * Returns the Dispatch operation code.
	 * @return the code
	 */
	public String getCode() {
		return _code;
	}
	
	public static DispatchRequest fromType(String type) {
		for (DispatchRequest dr : values()) {
			if (dr.getCode().equalsIgnoreCase(type))
				return dr;
		}
		
		return UNKNOWN;
	}
}