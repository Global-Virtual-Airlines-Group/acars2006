// Copyright 2015, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

/**
 * An ACARS Voice ping interval udpate message.
 * @author Luke
 * @version 8.4
 * @since 6.2
 */

public class VoicePingIntervalMessage extends AbstractMessage implements IntervalMessage {
	
	private final int _updateInterval;

	/**
	 * Creates the message.
	 * @param interval the update interval in seconds
	 */
	public VoicePingIntervalMessage(int interval) {
		super(MessageType.VOICEPINGINT, null);
		_updateInterval = Math.max(5, interval);
	}

	/**
	 * Returns the UDP ping interval.
	 * @return the interval in seconds
	 */
	@Override
	public int getInterval() {
		return _updateInterval;
	}
}