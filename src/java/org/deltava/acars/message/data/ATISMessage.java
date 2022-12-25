// Copyright 2022 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.acars.message.*;

import org.deltava.beans.Pilot;
import org.deltava.beans.navdata.ATIS;

/**
 * An ACARS data response message to store ATIS data.
 * @author Luke
 * @version 10.3
 * @since 10.3
 */

public class ATISMessage extends DataResponseMessage<ATIS> {

	/**
	 * Instantiates the Message.
	 * @param msgFrom the originating Pilot
	 * @param parentID the parent messge ID
	 */
	public ATISMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, DataRequest.ATIS, parentID);
	}
}