// Copyright 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import java.util.*;

import org.deltava.beans.Pilot;
import org.deltava.beans.acars.TaskTimerData;

/**
 * An ACARS message to store client performance data.
 * @author Luke
 * @version 8.6
 * @since 8.6
 */

public class PerformanceMessage extends AbstractMessage {
	
	private int _flightID;
	private final Collection<TaskTimerData> _tt = new LinkedHashSet<TaskTimerData>();

	/**
	 * Creates the message.
	 * @param msgFrom the originating Pilot
	 */
	public PerformanceMessage(Pilot msgFrom) {
		super(MessageType.PERFORMANCE, msgFrom);
	}

	/**
	 * Returns the Flight ID.
	 * @return the ID
	 */
	public int getFlightID() {
		return _flightID;
	}
	
	/**
	 * Returns the task timer data.
	 * @return a Collection of TaskTimerData beans
	 */
	public Collection<TaskTimerData> getTimers() {
		return _tt;
	}

	/**
	 * Adds task timer data to this message.
	 * @param tt a TaskTimerData
	 */
	public void addTimerData(TaskTimerData tt) {
		_tt.add(tt);
	}

	/**
	 * Updates the Flight ID for this data collection.
	 * @param id the flight ID
	 */
	public void setFlightID(int id) {
		_flightID = id;
	}
}