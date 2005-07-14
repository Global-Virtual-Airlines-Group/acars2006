package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * Specialized acknowledgement message.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */
public class ErrorMessage extends AcknowledgeMessage {

	private String errMsg;
	
	public ErrorMessage(Pilot msgTo, long parentID) {
		super(msgTo, parentID);
	}
	
	public String getText() {
		return this.errMsg;
	}
	
	public void setText(String msg) {
		this.errMsg = msg;
	}
}