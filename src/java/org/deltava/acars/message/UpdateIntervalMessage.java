// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * An ACARS message to update the Pilot Client's non-logged position interval.
 * @author Luke
 * @version 4.0
 * @since 4.0
 */

public class UpdateIntervalMessage extends AbstractMessage {
	
	private int _interval;

	/**
	 * Creates the message.
	 * @param msgFrom the sending Pilot
	 * @param interval the update interval in milliseconds
	 */
	public UpdateIntervalMessage(Pilot msgFrom, int interval) {
		super(Message.MSG_POSUPDINT, msgFrom);
		_interval = Math.max(150, interval);
	}

	/**
	 * Returns the update interval.
	 * @return the update interval in milliseconds
	 */
	public int getInterval() {
		return _interval;
	}
}