// Copyright 2007, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.dispatch;

import org.deltava.beans.Pilot;
import org.deltava.acars.message.DispatchMessage;
import org.deltava.acars.message.DispatchRequest;

/**
 * An ACARS message to request progress information about a flight.
 * @author Luke
 * @version 8.4
 * @since 2.1
 */

public class ProgressRequestMessage extends DispatchMessage {

	/**
	 * Initializes the Message.
	 * @param msgFrom the originating user
	 * @param userID the Pilot ID of the requested Pilot
	 */
	public ProgressRequestMessage(Pilot msgFrom, String userID) {
		super(DispatchRequest.PROGRESS, msgFrom);
		setRecipient(userID);
	}
}