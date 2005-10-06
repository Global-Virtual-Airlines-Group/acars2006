package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * Error message. This is an internally generated message and not part of the ACARS wire protocol.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class ErrorMessage extends AbstractMessage {

	private String _errMsg;
	
	public ErrorMessage(Pilot msgTo, String msg) {
		super(Message.MSG_ERROR, msgTo);
		_errMsg = msg;
	}
	
	public ErrorMessage(Pilot msgTo, String msg, long id) {
		this(msgTo, msg);
		setID(id);
	}
	
	public String getText() {
		return _errMsg;
	}
	
	public void setText(String msg) {
		_errMsg = msg;
	}
}