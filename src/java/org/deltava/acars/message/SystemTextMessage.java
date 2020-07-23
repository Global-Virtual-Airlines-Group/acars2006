// Copyright 2005, 2006, 2008, 2017, 2018, 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import java.util.*;

/**
 * A class to store ACARS server messages.
 * @author Luke
 * @version 9.1
 * @since 1.0
 */

public class SystemTextMessage extends AbstractMessage {
	
	private final Collection<String> _msgs = new ArrayList<String>();
	private boolean _isWarning;

	/**
	 * Creates the message.
	 */
	public SystemTextMessage() {
		super(MessageType.SYSTEM, null);
	}

	/**
	 * Adds a server message to the Message.
	 * @param msg the message text
	 */
	public void addMessage(String msg) {
		_msgs.add(msg);
	}
	
	/**
	 * Returns whether this is a warning message.
	 * @return TRUE if a warning message, otherwise FALSE
	 */
	public boolean isWarning() {
		return _isWarning;
	}
	
	/**
	 * Returns the server messages.
	 * @return a Collection of messages
	 */
	public Collection<String> getMessages() {
		return _msgs;
	}
	
	/**
	 * Sets whetehr this is a warning message.
	 * @param isWarn TRUE if a warning message, otherwise FALSE
	 */
	public void setWarning(boolean isWarn) {
		_isWarning = isWarn;
	}
}