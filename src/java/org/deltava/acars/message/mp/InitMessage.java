// Copyright 2008, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.mp;

import org.deltava.acars.message.*;

import org.deltava.beans.Pilot;

/**
 * An ACARS multi-player position initialization message.
 * @author Luke
 * @version 2.8
 * @since 2.2
 */

public class InitMessage extends AbstractMessage {
	
	/**
	 * Initializes the Message.
	 * @param msgFrom the originating Pilot
	 */
	public InitMessage(Pilot msgFrom) {
		super(Message.MSG_MPINIT, msgFrom);
		setProtocolVersion(2);
	}
}