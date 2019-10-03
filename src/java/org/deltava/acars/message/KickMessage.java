// Copyright 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * An ACARS message to disconnect another user. 
 * @author Luke
 * @version 8.7
 * @since 8.7
 */

public class KickMessage extends RecipientMessage {
	
	private final boolean _blockUser;

	/**
	 * Creates the message.
	 * @param msgFrom the originating Pilot
	 * @param blockUser TRUE to block the user from reconnecting, otherwise FALSE
	 */
	public KickMessage(Pilot msgFrom, boolean blockUser) {
		super(MessageType.DISCONNECT, msgFrom);
		_blockUser = blockUser;
	}

	/**
	 * Returns whether the user should be blocked from reconnecting.
	 * @return TRUE if blocked, otherwise FALSE
	 */
	public boolean getBlockUser() {
		return _blockUser;
	}
}