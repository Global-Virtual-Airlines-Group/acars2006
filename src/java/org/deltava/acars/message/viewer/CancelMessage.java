// Copyright 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.viewer;

import org.deltava.acars.message.*;

import org.deltava.beans.Pilot;

/**
 * An ACARS message to send Flight viewer cancellations.
 * @author Luke
 * @version 2.8
 * @since 2.8
 */

public class CancelMessage extends ViewerMessage {

	/**
	 * Creates the Message.
	 * @param msgFrom the originating Pilot
	 */
	public CancelMessage(Pilot msgFrom) {
		super(VIEW_CANCEL, msgFrom);
	}
}