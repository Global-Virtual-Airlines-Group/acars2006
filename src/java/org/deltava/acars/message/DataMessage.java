// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;
import org.deltava.util.StringUtils;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public abstract class DataMessage extends AbstractMessage {
	
	// Request type constants
	public static final int REQ_UNKNOWN = 0;
	public static final int REQ_USRLIST = 1;
	public static final int REQ_ILIST = 2;
	public static final int REQ_PLIST = 3;
	public static final int REQ_ADDUSER = 4;
	public static final int REQ_REMOVEUSER = 5;
	public static final int REQ_PILOTINFO = 6;
	public static final int REQ_NAVAIDINFO = 7;
	public static final int REQ_PVTVOX = 8;
	public static final int REQ_EQLIST = 9;
	public static final int REQ_APLIST = 10;
	public static final int REQ_ALLIST = 11;
	public static final int REQ_CHARTS = 12;
	public static final int REQ_ATCINFO = 13;
	public static final int REQ_BUSY = 14;
	public static final int REQ_DRAFTPIREP = 15;
	
	private int _reqType = REQ_UNKNOWN;
	public static final String[] REQ_TYPES = {"?", "pilots", "info", "position", "addpilots", "delpilots", "pilot", "navaid", "pvtvox",
		"eqList", "apList", "aList", "charts", "atc" , "busy", "draftpirep"};

	/**
	 * @param type
	 * @param msgFrom
	 */
	public DataMessage(int type, Pilot msgFrom) {
		super(type, msgFrom);
	}
	
	public int getRequestType() {
		return _reqType;
	}

	public void setRequestType(String newRT) {
		int reqType = StringUtils.arrayIndexOf(REQ_TYPES, newRT);
		setRequestType((reqType == -1) ? 0 : reqType);
	}
	
	public void setRequestType(int rType) {
		_reqType = rType;
	}
}