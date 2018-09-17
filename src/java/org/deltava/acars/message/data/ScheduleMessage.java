// Copyright 2006, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.acars.message.*;

import org.deltava.beans.Pilot;
import org.deltava.beans.schedule.ScheduleEntry;

/**
 * An ACARS data response message to store Schedule entries.
 * @author Luke
 * @version 8.4
 * @since 1.0
 */

public class ScheduleMessage extends DataResponseMessage<ScheduleEntry> {

	/**
	 * Instantiates the Message.
	 * @param msgFrom the originating Pilot
	 * @param parentID the request message ID
	 */
	public ScheduleMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, DataRequest.SCHED, parentID);
	}
}