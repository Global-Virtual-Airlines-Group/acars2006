// Copyright 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.beans.Pilot;
import org.deltava.beans.acars.Livery;

import org.deltava.acars.message.DataResponseMessage;

/**
 * An ACARS data response message to store multi-player livery data.
 * @author Luke
 * @version 2.2
 * @since 2.2
 */

public class LiveryMessage extends DataResponseMessage<Livery> {

	/**
	 * Instantiates the Message.
	 * @param msgFrom the originating Pilot
	 * @param parentID the parent messge ID
	 */
	public LiveryMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, REQ_LIVERIES, parentID);
	}
}