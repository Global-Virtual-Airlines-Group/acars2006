// Copyright 2007, 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

/**
 * A Worker status bean with a latency monitor.
 * @author Luke
 * @version 2.1
 * @since 2.0
 */

public class LatencyWorkerStatus extends WorkerStatus {
	
	private LatencyTracker _latency;

	/**
	 * Initializes the status bean.
	 * @param name the worker name
	 * @param sortOrder the sort ordering value
	 * @param entries the number of entries to track latency history for
	 */
	public LatencyWorkerStatus(String name, int sortOrder, int entries) {
		super(name, sortOrder);
		_latency = new LatencyTracker(entries);
	}

	/**
	 * Adds an operation's latency.
	 * @param latency the latency in milliseconds
	 */
	public void add(long latency) {
		_latency.add(latency);
	}
	/**
	 * Returns the average latency for all operations.
	 * @return the average latency in milliseconds
	 */
	public int getLatency() {
		return _latency.getLatency();
	}
	
	/**
	 * Returns the message and average latency.
	 */
	public String getMessage() {
		return super.getMessage() + " - average latency " + _latency.getLatency() + "ms";
	}
}