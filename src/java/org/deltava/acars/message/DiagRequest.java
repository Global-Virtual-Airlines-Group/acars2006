// Copyright 2018, 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

/**
 * An enumeration of Diagnostic message types.
 * @author Luke
 * @version 9.1
 * @since 8.4
 */

public enum DiagRequest implements SubRequest {
	UNKNOWN("Unknown"), KICK("KickUser"), BLOCK("BlockIP"), WARN("ContentWarn");
	
	private final String _code;
	
	/**
	 * Creates a new value.
	 * @param code the code
	 */
	DiagRequest(String code) {
		_code = code;
	}
	
	@Override
	public String getCode() {
		return _code;
	}
	
	@Override
	public final SubRequestType getType() {
		return SubRequestType.DIAG;
	}
	
	public static DiagRequest fromType(String type) {
		for (DiagRequest req : values()) {
			if (req._code.equalsIgnoreCase(type))
				return req;
		}
		
		return UNKNOWN;
	}
}