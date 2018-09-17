// Copyright 2004, 2005, 2007, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

import org.deltava.util.StringUtils;

/**
 * An ACARS super user message.
 * @author Luke
 * @version 8.4
 * @since 1.0
 */

public class DiagnosticMessage extends AbstractMessage {

	// Request type constants
	public static final int REQ_UNKNOWN = 0;
	public static final int REQ_KICK = 1;
	public static final int REQ_BLOCK = 2;
	public static final int CONTENT_WARN = 3;
	public static final String[] REQ_TYPES = {"?", "KickUser", "BlockIP", "ContentWarn"};

	// Request type
	private int _reqType;
	private String _reqData;

	public DiagnosticMessage(Pilot msgFrom) {
		super(MessageType.DIAG, msgFrom);
	}
	
	public String getRequestData() {
		return _reqData;
	}
	
	public int getRequestType() {
		return _reqType;
	}
	
	public void setRequestData(String newRD) {
		_reqData = newRD;
	}
	
	public void setRequestType(int newRT) {
		if ((newRT >= 0) && (newRT < REQ_TYPES.length))
			_reqType = newRT;
	}
	
	public void setRequestType(String newRT) {
		int rType = StringUtils.arrayIndexOf(REQ_TYPES, newRT);
		setRequestType((rType == -1) ? 0 : rType);
	}
}