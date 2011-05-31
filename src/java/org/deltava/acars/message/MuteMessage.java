// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * An ACARS message to mute/unmute a user.
 * @author Luke
 * @version 4.0
 * @since 4.0
 */

public class MuteMessage extends AbstractMessage {
	
	private String _recipient;

	/**
	 * Creates the message.
	 * @param msgFrom the originating Pilot
	 * @param recipient the user ID to mute
	 */
	public MuteMessage(Pilot msgFrom, String recipient) {
		super(Message.MSG_MUTE, msgFrom);
		_recipient = recipient;
	}

	/**
	 * Returns the recipient pilot ID.
	 * @return the Pilot ID
	 */
	public String getRecipient() {
		return _recipient;
	}
}