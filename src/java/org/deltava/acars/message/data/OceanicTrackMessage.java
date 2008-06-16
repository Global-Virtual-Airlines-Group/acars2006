// Copyright 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.beans.Pilot;
import org.deltava.beans.schedule.OceanicWaypoints;

import org.deltava.acars.message.*;

/**
 * An ACARS data response message to store Oceanic track data.
 * @author Luke
 * @version 2.2
 * @since 2.2
 */

public class OceanicTrackMessage extends DataResponseMessage<OceanicWaypoints> {

	/**
	 * Instantiates the Message.
	 * @param msgFrom the originating Pilot
	 * @param parentID the parent message ID
	 */
	public OceanicTrackMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, DataMessage.REQ_NATS, parentID);
	}
}