// Copyright 2004, 2009, 2011, 2018, 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/** 
 * An ACARS text message.
 * @author Luke
 * @version 10.1
 * @since 1.0
 */

public class TextMessage extends RecipientMessage {
	
	private final static int MAX_MSG_SIZE = 1024;

	private final String _text;
	private String _channel;

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
	 * Returns the channel name.
	 * @return the name, or null if none
	 */
	public String getChannel() {
		return _channel;
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

	/**
	 * Updates the channel name.
	 * @param ch the channel, or null if none
	 */
	public void setChannel(String ch) {
		_channel = ch;
	}
}