// Copyright 2011, 2015, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * An ACARS message to update the Pilot Client's non-logged position interval.
 * @author Luke
 * @version 8.4
 * @since 4.0
 */

public class UpdateIntervalMessage extends AbstractMessage implements IntervalMessage {
	
	private final int _interval;

	/**
	 * Creates the message.
	 * @param msgFrom the sending Pilot
	 * @param interval the update interval in milliseconds
	 */
	public UpdateIntervalMessage(Pilot msgFrom, int interval) {
		super(MessageType.POSUPDINT, msgFrom);
		_interval = Math.max(125, interval);
	}

	/**
	 * Returns the update interval.
	 * @return the update interval in milliseconds
	 */
	@Override
	public int getInterval() {
		return _interval;
	}
}