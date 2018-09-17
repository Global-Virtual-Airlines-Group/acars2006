// Copyright 2007, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.dispatch;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.*;

/**
 * An ACARS message to transmit dispatch cancellation.
 * @author Luke
 * @version 8.4
 * @since 2.0
 */

public class CancelMessage extends DispatchMessage {

	/**
	 * Creates the message.
	 * @param msgFrom the originating Pilot
	 */
	public CancelMessage(Pilot msgFrom) {
		super(DispatchRequest.CANCEL, msgFrom);
	}
}