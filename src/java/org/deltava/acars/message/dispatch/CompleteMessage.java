// Copyright 2007, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.dispatch;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.*;

/**
 * A message to transmit dispatch completion acknowledgement.
 * @author luke
 * @version 8.4
 * @since 2.0
 */

public class CompleteMessage extends DispatchMessage {

	/**
	 * Initializes the Message.
	 * @param msgFrom the originating Pilot
	 */
	public CompleteMessage(Pilot msgFrom) {
		super(DispatchRequest.COMPLETE, msgFrom);
	}
}