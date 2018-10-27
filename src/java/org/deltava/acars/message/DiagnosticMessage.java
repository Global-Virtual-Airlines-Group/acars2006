// Copyright 2004, 2005, 2007, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * An ACARS super user message.
 * @author Luke
 * @version 8.4
 * @since 1.0
 */

public class DiagnosticMessage extends AbstractMessage {

	// Request type
	private DiagRequest _reqType;
	private String _reqData;

	public DiagnosticMessage(Pilot msgFrom) {
		super(MessageType.DIAG, msgFrom);
	}
	
	public String getRequestData() {
		return _reqData;
	}
	
	public DiagRequest getRequestType() {
		return _reqType;
	}
	
	public void setRequestData(String newRD) {
		_reqData = newRD;
	}
	
	public void setRequestType(DiagRequest newRT) {
		_reqType = newRT;
	}
}