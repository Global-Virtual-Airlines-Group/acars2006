// Copyright 2019, 2022 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import java.util.*;

import org.deltava.beans.Pilot;
import org.deltava.beans.acars.*;

/**
 * An ACARS message to store client performance data.
 * @author Luke
 * @version 10.2
 * @since 8.6
 */

public class PerformanceMessage extends AbstractMessage {
	
	private int _flightID;
	private final Collection<TaskTimerData> _tt = new LinkedHashSet<TaskTimerData>();
	private final Map<String, Integer> _ctrs = new HashMap<String, Integer>();
	private FrameRates _frames;

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
	 * Returns the Frame Rate data.
	 * @return a FrameRates bean
	 */
	public FrameRates getFrames() {
		return _frames;
	}
	
	/**
	 * Returns the performance counter names.
	 * @return a Collection of counter names
	 */
	public Collection<String> getCounters() {
		return _ctrs.keySet();
	}
	
	/**
	 * Returns a performance counter value.
	 * @param key the counter name
	 * @param defaultValue the default value if the counter does not exist
	 * @return the counter value, or the default
	 */
	public int getCounter(String key, int defaultValue) {
		return _ctrs.getOrDefault(key, Integer.valueOf(defaultValue)).intValue();
	}
	
	/**
	 * Returns the task timer data.
	 * @return a Collection of TaskTimerData beans
	 */
	public Collection<TaskTimerData> getTimers() {
		return _tt;
	}
	
	/**
	 * Returns if there are any performance counter values.
	 * @return TRUE if there is at least one counter value, otherwise FALSE
	 */
	public boolean hasCounters() {
		return (_ctrs.size() > 0);
	}
	
	/**
	 * Returns if there are any task timer values.
	 * @return TRUE if there is at least one timer value, otherwise FALSE
	 */
	public boolean hasTimers() {
		return (_tt.size() > 0);
	}

	/**
	 * Adds task timer data to this message.
	 * @param tt a TaskTimerData
	 */
	public void addTimerData(TaskTimerData tt) {
		_tt.add(tt);
	}
	
	/**
	 * Adds an integer performance counter.
	 * @param label the counter name
	 * @param value the counter value
	 */
	public void addCounter(String label, int value) {
		_ctrs.put(label, Integer.valueOf(value));
	}
	
	/**
	 * Updates the Flight ID for this data collection.
	 * @param id the flight ID
	 */
	public void setFlightID(int id) {
		_flightID = id;
	}
	
	/**
	 * Updates the frame rate data.
	 * @param fr a FrameRates bean
	 */
	public void setFrames(FrameRates fr) {
		if (fr != null) fr.setID(_flightID);
		_frames = fr;
	}
}