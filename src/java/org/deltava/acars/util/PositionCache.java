// Copyright 2017, 2024 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.util;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import org.deltava.beans.*;
import org.deltava.util.*;

import org.deltava.acars.message.PositionMessage;

/**
 * An in-memory cache for PositionMessages and TrackUpdates.
 * @author Luke
 * @version 11.3
 * @param <T> the cached update
 * @since 7.3
 */

@Helper(PositionMessage.class)
public class PositionCache<T extends GeoLocation> {
	
	private final BlockingQueue<T> _queue = new LinkedBlockingQueue<T>();
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
	 * @param loc the bean
	 */
	public void queue(T loc) {
		if (_queue.isEmpty())
			_oldestAge.set(System.currentTimeMillis());
		
		// Don't add 0/0 pairs
		if (GeoUtils.isValid(loc))
			_queue.add(loc);
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
	 * @return a Collection of cached objects
	 */
	public Collection<T> drain() {
		Collection<T> results = new ArrayList<>();
		_queue.drainTo(results);
		return results;
	}
}