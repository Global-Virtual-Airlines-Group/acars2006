// Copyright 2005, 2011, 2018, 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import java.time.Instant;

import org.deltava.beans.Pilot;

/**
 * An ACARS ping message.
 * @author Luke
 * @version 9.1
 * @since 1.0
 */

public class PingMessage extends AbstractMessage {
	
	private Instant _localTime;

	/**
	 * Creates a new Ping Message.
	 * @param msgFrom the originating Pilot
	 */
	public PingMessage(Pilot msgFrom) {
		super(MessageType.PING, msgFrom);
	}
	
	@Override
	public boolean isAnonymous() {
		return false;
	}
	
	/**
	 * Returns the local time at the ACARS client.
	 * @return the local UTC time
	 */
	public Instant getClientUTC() {
		return _localTime;
	}
	
	/**
	 * Updates the local time at the ACARS client.
	 * @param dt the local UTC time
	 */
	public void setClientUTC(Instant dt) {
		_localTime = dt;
	}
}