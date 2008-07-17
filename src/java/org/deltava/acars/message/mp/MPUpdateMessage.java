// Copyright 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.mp;

import org.deltava.acars.message.DataResponseMessage;
import org.deltava.acars.message.LocationMessage;
import org.deltava.acars.message.Message;

/**
 * A Multi-Player message to handle batch updates of all aircraft positions.
 * @author Luke
 * @version 2.2
 * @since 2.2
 */

public class MPUpdateMessage extends DataResponseMessage<LocationMessage> {
	
	private boolean _doClear;

	/**
	 * Initializes the Message.
	 * @param doClear TRUE if the MP list should be cleared, otherwise FALSE
	 */
	public MPUpdateMessage(boolean doClear) {
		super(null, Message.MSG_MPUPDATE, 0);
		_doClear = doClear;
	}
	
	/**
	 * Returns whether the MP list should be cleared.
	 * @return TRUE if positions should be cleared, otherwise FALSE
	 */
	public boolean isClear() {
		return _doClear;
	}
}