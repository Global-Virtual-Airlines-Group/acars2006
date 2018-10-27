// Copyright 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

/**
 * An enumeration of Diagnostic message types.
 * @author Luke
 * @version 8.4
 * @since 8.4
 */

public enum DiagRequest {
	UNKNOWN("Unknown"), KICK("KickUser"), BLOCK("BlockIP"), WARN("ContentWarn");
	
	private final String _desc;
	
	/**
	 * Creates a new value.
	 * @param desc the description
	 */
	DiagRequest(String desc) {
		_desc = desc;
	}
	
	/**
	 * Returns the request type description.
	 * @return the description
	 */
	public String getDescription() {
		return _desc;
	}
	
	public static DiagRequest fromType(String type) {
		for (DiagRequest req : values()) {
			if (req.getDescription().equalsIgnoreCase(type))
				return req;
		}
		
		return UNKNOWN;
	}
}