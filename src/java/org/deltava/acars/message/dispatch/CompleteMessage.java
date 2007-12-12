// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.dispatch;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.DispatchMessage;

/**
 * A message to transmit dispatch completion acknowledgement.
 * @author luke
 * @version 2.0
 * @since 2.0
 */

public class CompleteMessage extends DispatchMessage {

	/**
	 * Initializes the Message.
	 * @param msgFrom
	 */
	public CompleteMessage(Pilot msgFrom) {
		super(DispatchMessage.DSP_COMPLETE, msgFrom);
	}
}