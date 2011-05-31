// Copyright 2004, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * Error message. This is an internally generated message and not part of the ACARS wire protocol.
 * @author Luke
 * @version 4.0
 * @since 1.0
 */

public class ErrorMessage extends AbstractMessage {

	private String _errMsg;

	/**
	 * Creates a new error message.
	 * @param msgTo the Pilot to send to 
	 * @param msg the error message
	 * @param id the message ID
	 */
	public ErrorMessage(Pilot msgTo, String msg, long id) {
		super(Message.MSG_ERROR, msgTo);
		_errMsg = msg;
		setID(id);
	}

	/**
	 * Returns the error message text.
	 * @return the message text
	 */
	public String getText() {
		return _errMsg;
	}
}