// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.dispatch;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.DispatchMessage;

/**
 * An ACARS message to transmit dispatch cancellation.
 * @author Luke
 * @version 2.0
 * @since 2.0
 */

public class CancelMessage extends DispatchMessage {

	/**
	 * Creates the message.
	 * @param msgFrom the originating pilot
	 */
	public CancelMessage(Pilot msgFrom) {
		super(DSP_CANCEL, msgFrom);
	}
}