// Copyright 2005, 2011, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * An ACARS ping message.
 * @author Luke
 * @version 8.4
 * @since 1.0
 */

public class PINGMessage extends AbstractMessage {

	/**
	 * Creates a new Ping Message.
	 * @param msgFrom the originating Pilot
	 */
	public PINGMessage(Pilot msgFrom) {
		super(MessageType.PING, msgFrom);
	}
	
	/**
	 * Returns whether the message can be sent by an unauthenticated user.
	 */
	@Override
	public boolean isAnonymous() {
		return false;
	}
}