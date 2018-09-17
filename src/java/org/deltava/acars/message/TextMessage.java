// Copyright 2004, 2009, 2011, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/** 
 * An ACARS text message.
 * @author Luke
 * @version 8.4
 * @since 1.0
 */

public class TextMessage extends RecipientMessage {
	private final static int MAX_MSG_SIZE = 1024;

	private final String _text;

	/**
	 * Creates the message.
	 * @param msgFrom the originating Pilot
	 * @param msgText the message text
	 */
	public TextMessage(Pilot msgFrom, String msgText) {
		super(MessageType.TEXT, msgFrom);
		_text = (msgText.length() > MAX_MSG_SIZE) ? msgText.substring(0, MAX_MSG_SIZE) : msgText;
	}
	
	/**
	 * Returns the message text.
	 * @return the text
	 */
	public String getText() {
		return _text; 
	}
	
	/**
	 * Returns whether the message is public. 
	 * @return TRUE if no recipient, otherwise FALSE
	 */
	public final boolean isPublic() {
		return (getRecipient() == null);
	}
}