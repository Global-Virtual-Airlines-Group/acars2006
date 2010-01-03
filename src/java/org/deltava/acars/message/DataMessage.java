// Copyright 2005 2006, 2008, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;
import org.deltava.util.StringUtils;

/**
 * An ACARS data request/response message.
 * @author Luke
 * @version 2.8
 * @since 1.0
 */

public abstract class DataMessage extends AbstractMessage {
	
	// Request type constants
	public static final int REQ_UNKNOWN = 0;
	public static final int REQ_USRLIST = 1;
	
	/**
	 * @deprecated
	 */
	public static final int REQ_ILIST = 2;
	
	public static final int REQ_VALIDATE = 3;
	public static final int REQ_ADDUSER = 4;
	public static final int REQ_REMOVEUSER = 5;
	public static final int REQ_TRINFO = 6;
	public static final int REQ_NAVAIDINFO = 7;
	public static final int REQ_PVTVOX = 8;
	public static final int REQ_EQLIST = 9;
	public static final int REQ_APLIST = 10;
	public static final int REQ_ALLIST = 11;
	public static final int REQ_CHARTS = 12;
	public static final int REQ_ATCINFO = 13;
	public static final int REQ_BUSY = 14;
	public static final int REQ_DRAFTPIREP = 15;
	public static final int REQ_TS2SERVERS = 16;
	public static final int REQ_SCHED = 17;
	public static final int REQ_NATS = 18;
	public static final int REQ_HIDE = 19;
	public static final int REQ_LIVERIES = 20;
	public static final int REQ_WX = 21;
	public static final int REQ_APINFO = 22;
	
	private int _reqType = REQ_UNKNOWN;
	public static final String[] REQ_TYPES = {"?", "pilots", "info", "validate", "addpilots", "delpilots", "sidstar", "navaid", "pvtvox",
		"eqList", "apList", "aList", "charts", "atc" , "busy", "draftpirep", "ts2servers", "sched", "nat", "hide", "liveries", "wx", "airportinfo"};

	/**
	 * Creates the message.
	 * @param type the message type
	 * @param msgFrom the originating user
	 */
	protected DataMessage(int type, Pilot msgFrom) {
		super(type, msgFrom);
	}
	
	/**
	 * Returns the request type.
	 * @return the request type code
	 * @see DataMessage#setRequestType(int)
	 * @see DataMessage#setRequestType(String)
	 */
	public int getRequestType() {
		return _reqType;
	}

	/**
	 * Sets the request/response type.
	 * @param newRT the request type
	 * @see DataMessage#setRequestType(int)
	 */
	public void setRequestType(String newRT) {
		int reqType = StringUtils.arrayIndexOf(REQ_TYPES, newRT);
		setRequestType((reqType == -1) ? 0 : reqType);
	}
	
	/**
	 * Sets the request/response type.
	 * @param rType the reuqest type code
	 * @see DataMessage#setRequestType(String)
	 */
	public void setRequestType(int rType) {
		_reqType = rType;
	}
}