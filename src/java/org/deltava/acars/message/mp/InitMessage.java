// Copyright 2008, 2009, 2010 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.mp;

import org.deltava.acars.message.*;

import org.deltava.beans.Pilot;

/**
 * An ACARS multi-player position initialization message.
 * @author Luke
 * @version 3.0
 * @since 2.2
 */

public class InitMessage extends AbstractMessage {
	
	private int _range;
	
	/**
	 * Initializes the Message.
	 * @param msgFrom the originating Pilot
	 */
	public InitMessage(Pilot msgFrom) {
		super(Message.MSG_MPINIT, msgFrom);
		setProtocolVersion(2);
	}
	
	/**
	 * Returns the multi-player range.
	 * @return the range in miles
	 */
	public int getRange() {
		return _range;
	}
	
	/**
	 * Sets the initiali multi-player range.
	 * @param range the range in miles
	 */
	public void setRange(int range) {
		_range = Math.max(0, range);
	}
}