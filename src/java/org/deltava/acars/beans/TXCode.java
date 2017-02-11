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
	
	public static final int DEFAULT_VFR = 1200;
	public static final int DEFAULT_IFR = 2200;

	private final int _code;
	private Instant _assignedOn;
	
	/**
	 * Returns whether a transponder code is a default IFR/VFR code.
	 * @param txCode the transponder code
	 * @return TRUE if a default code, otherwise FALSE
	 */
	public static boolean isDefault(int txCode) {
		return ((txCode == DEFAULT_IFR) || (txCode == DEFAULT_VFR));
	}
	
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