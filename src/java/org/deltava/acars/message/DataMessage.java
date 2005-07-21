// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public abstract class DataMessage extends AbstractMessage {
	
	// Request type constants
	public static final int REQ_UNKNOWN = 0;
	public static final int REQ_ALIST = 1;
	public static final int REQ_ILIST = 2;
	public static final int REQ_PLIST = 3;
	public static final int REQ_ADDUSER = 4;
	public static final int REQ_REMOVEUSER = 5;
	public static final int REQ_PILOTINFO = 6;
	public static final int REQ_NAVAIDINFO = 7;
	
	public static final String[] REQ_TYPES = {"?", "pilots", "info", "position", "addpilots", "delpilots", "pilot", "navaid"};
	private int _reqType = REQ_UNKNOWN;

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
		for (int x = 0; x < REQ_TYPES.length; x++) {
			if (REQ_TYPES[x].equals(newRT)) {
				_reqType = x;
				break;
			}
		}
	}
	
	public void setRequestType(int rType) {
		_reqType = rType;
	}
}
