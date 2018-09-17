// Copyright 2011, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.beans.Pilot;
import org.deltava.beans.system.AirlineInformation;

import org.deltava.acars.message.*;

/**
 * An ACARS data response message to store Application data.
 * @author Luke
 * @version 8.4
 * @since 3.6
 */

public class AppInfoMessage extends DataResponseMessage<AirlineInformation> {

	/**
	 * Instantiates the Message.
	 * @param msgFrom the originating Pilot
	 * @param parentID the parent message ID
	 */
	public AppInfoMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, DataRequest.APPINFO, parentID);
	}
}