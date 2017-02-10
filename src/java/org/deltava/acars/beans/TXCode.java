// Copyright 2017 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import java.time.Instant;

import org.deltava.beans.*;
import org.deltava.util.StringUtils;

/**
 * A bean to store Transponder codes.
 * @author Luke
 * @version 7.2
 * @since 7.2
 */

public class TXCode extends DatabaseBean implements ViewEntry {

	private final int _code;
	private Instant _assignedOn;
	
	/**
	 * Creates a new transponder code bean.
	 * @param code the transponder code
	 */
	public TXCode(int code) {
		super();
		_code = code;
	}

	/**
	 * Returns the transponder code.
	 * @return the code
	 */
	public int getCode() {
		return _code;
	}
	
	/**
	 * Returns the time the code was assigned.
	 * @return the date/time
	 */
	public Instant getAssignedOn() {
		return _assignedOn;
	}
	
	/**
	 * Updates the assignment date of the squawk code.
	 * @param dt the date/time
	 */
	public void setAssignedOn(Instant dt) {
		_assignedOn = dt;
	}
	
	@Override
	public Object cacheKey() {
		return Integer.valueOf(_code);
	}
	
	@Override
	public String getRowClassName() {
		return (_code >= 7000) ? "warn" : null;
	}
	
	@Override
	public String toString() {
		return StringUtils.format(_code, "0000");
	}
}