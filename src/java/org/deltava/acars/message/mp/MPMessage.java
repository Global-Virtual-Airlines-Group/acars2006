// Copyright 2008, 2009, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.mp;

import org.deltava.acars.message.*;

import org.deltava.beans.Pilot;

/**
 * A message to track multiplayer and Flight Viewer data.
 * @author Luke
 * @version 8.4
 * @since 2.2
 */

public class MPMessage extends LocationMessage {
	
	/**
	 * Initializes the message.
	 * @param msgFrom the originating Pilot
	 */
	public MPMessage(Pilot msgFrom) {
		super(MessageType.MPUPDATE, msgFrom);
		setProtocolVersion(2);
	}
}