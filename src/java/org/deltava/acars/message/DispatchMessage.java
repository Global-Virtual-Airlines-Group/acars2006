// Copyright 2006, 2007, 2008, 2009, 2010, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

import org.deltava.util.StringUtils;

/**
 * An ACARS message to store Dispatch information.
 * @author Luke
 * @version 4.0
 * @since 1.0
 */

public abstract class DispatchMessage extends RecipientMessage {
	
	private int _reqType = DSP_UNKNOWN;
	
	// Request type constants
	public static final int DSP_UNKNOWN = 0;
	public static final int DSP_SVCREQ = 1;
	public static final int DSP_CANCEL = 2;
	public static final int DSP_ACCEPT = 3;
	public static final int DSP_INFO = 4;
	public static final int DSP_ROUTEREQ = 5;
	public static final int DSP_ROUTEDATA = 6;
	public static final int DSP_COMPLETE = 7;
	public static final int DSP_PROGRESS = 8;
	public static final int DSP_RANGE = 9;
	public static final int DSP_SCOPEINFO = 10;
	public static final int DSP_ROUTEPLOT = 11;
	public static final String[] REQ_TYPES = {"?", "svcrequest", "cancel", "accept", "flightinfo", "routerequest", "routes", 
		"done", "progress", "svcrange", "scopeinfo", "rtplot"};
	
	/**
	 * Creates the message.
	 * @param dspType the dispatch message type
	 * @param msgFrom the originating user
	 */
	protected DispatchMessage(int dspType, Pilot msgFrom) {
		super(MSG_DISPATCH, msgFrom);
		_reqType = dspType;
	}
	
	/**
	 * Returns the request type.
	 * @return the request type code
	 * @see DispatchMessage#setRequestType(int)
	 * @see DispatchMessage#setRequestType(String)
	 */
	public int getRequestType() {
		return _reqType;
	}
	
	/**
	 * Returns the request type code.
	 * @return the request code
	 */
	public String getRequestTypeName() {
		return REQ_TYPES[_reqType];
	}
	
	/**
	 * Sets the request/response type.
	 * @param newRT the request type
	 * @see DispatchMessage#setRequestType(int)
	 */
	public void setRequestType(String newRT) {
		setRequestType(StringUtils.arrayIndexOf(REQ_TYPES, newRT, 0));
	}

	/**
	 * Sets the request/response type.
	 * @param rType the reuqest type code
	 * @see DispatchMessage#setRequestType(String)
	 */
	public void setRequestType(int rType) {
		_reqType = rType;
	}
}