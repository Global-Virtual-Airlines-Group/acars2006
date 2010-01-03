// Copyright 2008, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.mp;

import org.deltava.acars.message.*;

import org.deltava.beans.Pilot;

/**
 * A message to track multiplayer and Flight Viewer data.
 * @author Luke
 * @version 2.8
 * @since 2.2
 */

public class MPMessage extends LocationMessage {
	
	/**
	 * Initializes the message.
	 * @param msgFrom the originating Pilot
	 */
	public MPMessage(Pilot msgFrom) {
		super(Message.MSG_MPUPDATE, msgFrom);
		setProtocolVersion(2);
	}
}