/*
 * Created on Feb 6, 2004
 */
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * @author Luke J. Koln
 */
public class DiagnosticMessage extends AbstractMessage {

	// Request type constants
	public static final int REQ_UNKNOWN = -1;
	public static final int REQ_STATS = 0;	
	public static final int REQ_SHUTDOWN = 1;
	public static final int REQ_RECYCLE = 2;
	public static final int REQ_KICK = 3;
	public static final int REQ_BLOCK = 4;
	public static final String[] REQ_TYPES = {"Statistics", "Shutdown", "Recycle", "KickUser", "BlockIP"};

	// Request type
	private int reqType = REQ_UNKNOWN;
	private String reqData;

	public DiagnosticMessage(Pilot msgFrom) {
		super(Message.MSG_DIAG, msgFrom);
	}
	
	public String getRequestData() {
		return this.reqData;
	}
	
	public int getRequestType() {
		return this.reqType;
	}
	
	public void setRequestData(String newRD) {
		this.reqData = newRD;
	}
	
	public void setRequestType(int newRT) {
		if ((newRT >= 0) && (newRT < REQ_TYPES.length))
			this.reqType = newRT;
	}
	
	public void setRequestType(String newRT) {
		
		for (int x = 0; x < REQ_TYPES.length; x++) {
			if (REQ_TYPES[x].equals(newRT)) {
				this.reqType = x;
				break;
			}
		}
	}
	

}
