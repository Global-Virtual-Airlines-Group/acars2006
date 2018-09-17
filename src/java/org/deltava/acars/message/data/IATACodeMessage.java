// Copyright 2013, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.beans.Pilot;
import org.deltava.beans.acars.IATACodes;

import org.deltava.acars.message.*;

/**
 * An ACARS data response message to store IATA aircraft codes.
 * @author Luke
 * @version 8.4
 * @since 5.1
 */

public class IATACodeMessage extends DataResponseMessage<IATACodes> {

	/**
	 * Instantiates the Message.
	 * @param msgFrom the originating Pilot
	 * @param parentID the request message ID
	 */
	public IATACodeMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, DataRequest.IATA, parentID);
	}
}