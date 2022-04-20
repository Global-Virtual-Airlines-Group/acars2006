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
	private final Map<String, Number> _ctrs = new HashMap<String, Number>();
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
	public Number getCounter(String key, Number defaultValue) {
		return _ctrs.getOrDefault(key, defaultValue);
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
	 * Adds an integer performance counter.
	 * @param label the counter name
	 * @param value the counter value
	 */
	public void addCounter(String label, int value) {
		_ctrs.put(label, Integer.valueOf(value));
	}
	
	/**
	 * Adds a floating point performance counter.
	 * @param label the counter name
	 * @param value the counter value
	 */
	public void addCounter(String label, double value) {
		_ctrs.put(label, Double.valueOf(value));
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