// Copyright 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.mp;

import org.deltava.acars.message.*;

import org.deltava.beans.Pilot;

/**
 * A message to track multiplayer data.
 * @author Luke
 * @version 2.2
 * @since 2.2
 */

public class MPMessage extends LocationMessage {
	
	/**
	 * Initializes the message.
	 * @param msgFrom the originating Pilot
	 */
	public MPMessage(Pilot msgFrom) {
		super(Message.MSG_MPUPDATE, msgFrom);
	}
}