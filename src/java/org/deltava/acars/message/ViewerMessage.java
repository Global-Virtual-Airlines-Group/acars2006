// Copyright 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;
import org.deltava.util.StringUtils;

/**
 * An ACARS message to store Flight Viewer information.
 * @author Luke
 * @version 2.8
 * @since 2.8
 */

public abstract class ViewerMessage extends AbstractMessage implements RecipientMessage {
	
	private String _recipient;
	private int _reqType = VIEW_UNKNOWN;
	
	// Message type constants
	public static final int VIEW_UNKNOWN = 0;
	public static final int VIEW_REQ = 1;
	public static final int VIEW_ACCEPT = 2;
	public static final int VIEW_CANCEL = 3;
	public static final String[] REQ_TYPES = {"?", "request", "accept", "cancel"};

	/**
	 * Creates the message.
	 * @param dspType the viewer message type
	 * @param msgFrom the originating user
	 */
	protected ViewerMessage(int reqType, Pilot msgFrom) {
		super(Message.MSG_VIEWER, msgFrom);
		_reqType = reqType;
		setProtocolVersion(2);
	}

	/**
	 * Returns the request type.
	 * @return the request type code
	 * @see ViewerMessage#setRequestType(int)
	 * @see ViewerMessage#setRequestType(String)
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
	 * Returns the recipient pilot code.
	 * @return the pilot code
	 */
	public String getRecipient() {
		return _recipient;
	}

	/**
	 * Sets the request/response type.
	 * @param newRT the request type
	 * @see ViewerMessage#setRequestType(int)
	 */
	public void setRequestType(String newRT) {
		setRequestType(StringUtils.arrayIndexOf(REQ_TYPES, newRT, 0));
	}

	/**
	 * Sets the request/response type.
	 * @param rType the reuqest type code
	 * @see ViewerMessage#setRequestType(String)
	 */
	public void setRequestType(int rType) {
		_reqType = rType;
	}
	
	/**
	 * Updates the recipient of this message.
	 * @param msgTo the recipient pilot code
	 */
	public void setRecipient(String msgTo) {
		_recipient = msgTo;
	}
}