// Copyright 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.acars.message.DataResponseMessage;

import org.deltava.beans.Pilot;
import org.deltava.beans.schedule.ScheduleEntry;

/**
 * An ACARS data response message to store Schedule entries.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class ScheduleMessage extends DataResponseMessage<ScheduleEntry> {

	/**
	 * Instantiates the Message.
	 * @param msgFrom the originating Pilot
	 * @param parentID the request message ID
	 */
	public ScheduleMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, REQ_SCHED, parentID);
	}
}