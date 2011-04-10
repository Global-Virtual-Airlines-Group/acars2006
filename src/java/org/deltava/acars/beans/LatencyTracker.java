// Copyright 2007, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import java.util.*;

/**
 * A bean to track message processing latency.
 * @author Luke
 * @version 3.6
 * @since 1.0
 */

public class LatencyTracker {

	private final List<Long> _entries = new ArrayList<Long>();
	private int _maxSize;
	
	/**
	 * Initializes the tracker.
	 * @param maxEntries the number of operations to track
	 */
	public LatencyTracker(int maxEntries) {
		super();
		_maxSize = (maxEntries < 1) ? 1 : maxEntries;
	}

	/**
	 * Adds an operation's latency.
	 * @param latency the latency in milliseconds
	 */
	public synchronized void add(long latency) {
		_entries.add(Long.valueOf(latency));
		while (_entries.size() > _maxSize)
			_entries.remove(0);
	}
	
	/**
	 * Clears the current list of latency times.
	 */
	public synchronized void clear() {
		_entries.clear();
	}

	/**
	 * Returns the average latency for all operations.
	 * @return the average latency in milliseconds
	 */
	public synchronized long getLatency() {
		if (_entries.isEmpty())
			return 0;
		
		// Calculate the total
		long total = 0;
		for (int x = 0; x < _entries.size(); x++)
			total += _entries.get(x).longValue();
		
		// Divide by the entries
		return (total / _entries.size());
	}
}