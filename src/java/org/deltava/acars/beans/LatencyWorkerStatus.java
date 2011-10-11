// Copyright 2007, 2008, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import static java.util.concurrent.TimeUnit.*;

import org.deltava.util.StringUtils;

import org.gvagroup.ipc.WorkerStatus;

/**
 * A Worker status bean with a latency monitor.
 * @author Luke
 * @version 4.1
 * @since 2.0
 */

public class LatencyWorkerStatus extends WorkerStatus {
	
	private final LatencyTracker _latency;

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
	 * Clears the latency statistics.
	 */
	public void clear() {
		_latency.clear();
	}
	
	/**
	 * Returns the message and average latency.
	 */
	@Override
	public synchronized String getMessage() {
		long us = MICROSECONDS.convert(_latency.getLatency(), NANOSECONDS);
		StringBuilder buf = new StringBuilder(super.getMessage());
		buf.append(" - average latency ");
		buf.append(StringUtils.format(us / 1000.0d, "0.00"));
		buf.append("ms");
		return buf.toString();
	}
}