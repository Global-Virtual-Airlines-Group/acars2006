// Copyright 2018, 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

/**
 * An enumeration of Dispatch operation types. 
 * @author Luke
 * @version 9.1
 * @since 8.4
 */

public enum DispatchRequest implements SubRequest {
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
	
	@Override
	public final SubRequestType getType() {
		return SubRequestType.DISPATCH;
	}

	@Override
	public String getCode() {
		return _code;
	}
	
	public static DispatchRequest fromType(String type) {
		for (DispatchRequest dr : values()) {
			if (dr._code.equalsIgnoreCase(type))
				return dr;
		}
		
		return UNKNOWN;
	}
}