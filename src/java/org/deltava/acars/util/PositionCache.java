// Copyright 2017 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.util;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import org.deltava.beans.Helper;
import org.deltava.util.*;

import org.deltava.acars.message.PositionMessage;

/**
 * An in-memory cache for PositionMessages.
 * @author Luke
 * @version 7.3
 * @since 7.3
 */

@Helper(PositionMessage.class)
public class PositionCache {
	
	private final BlockingQueue<PositionMessage> _queue = new LinkedBlockingQueue<>();
	private final AtomicLong _oldestAge = new AtomicLong(0);
	
	private final int _maxSize;
	private final int _maxAge;

	/**
	 * Creates the position cache.
	 * @param maxSize the maximum size of the cache before full
	 * @param maxAge the maximum age  of the cache before full, in milliseconds
	 */
	public PositionCache(int maxSize, int maxAge) {
		super();
		_maxSize = Math.max(1, maxSize);
		_maxAge = Math.max(1, maxAge);
	}

	/**
	 * Adds a Position report to the queue.
	 * @param msg the PositionMessage bean
	 */
	public void queue(PositionMessage msg) {
		if (_queue.isEmpty())
			_oldestAge.set(System.currentTimeMillis());
		
		// Don't add 0/0 pairs
		if (GeoUtils.isValid(msg))
			_queue.add(msg);
	}
	
	/**
	 * Returns whether the cache is full.
	 * @return TRUE if full, otherwise FALSE
	 */
	public boolean isFull() {
		if (_queue.isEmpty()) return false;
		
		long age = System.currentTimeMillis() - _oldestAge.get();
		return (age >= _maxAge) || (_queue.size() >= _maxSize);
	}
	
	/**
	 * Drains the cache.
	 * @return a Collection of PositionMessage beans
	 */
	public Collection<PositionMessage> drain() {
		Collection<PositionMessage> results = new ArrayList<>();
		_queue.drainTo(results);
		return results;
	}
}