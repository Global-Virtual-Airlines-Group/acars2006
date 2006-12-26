// Copyright 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.util;

import java.util.*;
import java.util.concurrent.*;

/**
 * A utility class to cache text messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class MessageCache<E> {

	protected final BlockingQueue<CacheEntry> _cache = new LinkedBlockingQueue<CacheEntry>();
	
	private int _flushThreshold;
	private int _flushInterval;
	private boolean _forceFlush;
	private long _lastAdd;

	public class CacheEntry {

		private long _conID;
		private int _auxID;
		private E _msg;

		CacheEntry(E msg, long conID, int auxID) {
			super();
			_msg = msg;
			_conID = conID;
			_auxID = auxID;
		}

		public E getMessage() {
			return _msg;
		}

		public long getConnectionID() {
			return _conID;
		}

		public int getAuxID() {
			return _auxID;
		}
	}

	/**
	 * Initializes the Message cache.
	 * @param threshold the number of entries to store before being eligible for flushing
	 * @param interval the minimum age of the last entry before being eligible for flushing
	 */
	public MessageCache(int threshold, int interval) {
		super();
		_flushThreshold = (threshold < 1) ? 1 : threshold;
		_flushInterval = (interval < 0) ? 0 : interval;
	}
	
	/**
	 * Initializes the Message cache. The cache will be eligible for flushing with a minimum of 1 entry.
	 */
	public MessageCache() {
		this(1, 0);
	}

	/**
	 * Adds a new text message to the cache.
	 * @param msg the Message
	 * @param connectionID the ACARS connection ID
	 * @param recipientID another linked database ID
	 */
	public void push(E msg, long connectionID, int auxID) {
		_cache.add(new CacheEntry(msg, connectionID, auxID));
		_lastAdd = System.currentTimeMillis();
	}

	/**
	 * Returns all entries from the cache, and resets the force flush indicator
	 * @return a Collection of CacheEntry beans
	 */
	public synchronized Collection<CacheEntry>drain() {
		Collection<CacheEntry> results = new ArrayList<CacheEntry>(_cache.size());
		_cache.drainTo(results);
		_forceFlush = false;
		return results;
	}

	/**
	 * Forces the cache to be marked as eligible for flushing if not empty.
	 */
	public synchronized void force() {
		_forceFlush = !_cache.isEmpty();
	}

	/**
	 * Returns wether the cache is eligible to be flushed.
	 * @return TRUE if the cache is eligible to be flushed, otherwise FALSE
	 */
	public synchronized boolean isDirty() {
		return (_cache.size() >= _flushThreshold) || (!_cache.isEmpty() && (_forceFlush || ((System.currentTimeMillis() - _lastAdd) >= _flushInterval)));
	}
}