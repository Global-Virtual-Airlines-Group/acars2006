// Copyright 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.util;

import java.util.*;

import org.deltava.acars.message.PositionMessage;

/**
 * A utility class to cache position messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public final class PositionCache {

	private static Set<PositionCacheEntry> _cache = new LinkedHashSet<PositionCacheEntry>();
	private static long _lastFlush;

	public static class PositionCacheEntry {

		private PositionMessage _msg;
		private long _conID;
		private int _flightID;

		PositionCacheEntry(PositionMessage msg, long cID, int fID) {
			super();
			_msg = msg;
			_conID = cID;
			_flightID = fID;
		}

		public PositionMessage getMessage() {
			return _msg;
		}

		public long getConnectionID() {
			return _conID;
		}

		public int getFlightID() {
			return _flightID;
		}

		public boolean equals(Object o) {
			PositionCacheEntry e2 = (PositionCacheEntry) o;
			if (_conID != e2._conID)
				return false;

			return _msg.getDate().equals(e2._msg.getDate());
		}
	}

	// singleton constructor
	private PositionCache() {
	}

	/**
	 * Adds a new position entry to the cache.
	 * @param msg the Position data
	 * @param conID the ACARS connection ID
	 * @param flightID the ACARS flight ID
	 */
	public static synchronized void push(PositionMessage msg, long conID, int flightID) {
		if ((msg != null) && (flightID != 0))
			_cache.add(new PositionCacheEntry(msg, conID, flightID));
	}

	/**
	 * Returns and removes the first entry out of the cache.
	 * @return the first cache entry, or null
	 */
	public static synchronized PositionCacheEntry pop() {
		if (_cache.isEmpty())
			return null;

		// Return the first entry
		Iterator<PositionCacheEntry> i = _cache.iterator();
		PositionCacheEntry entry = i.next();
		i.remove();
		return entry;
	}

	/**
	 * Returns the number of milliseconds since the last position flush.
	 * @return the number of millseconds
	 */
	public static long getFlushInterval() {
		return System.currentTimeMillis() - _lastFlush;
	}

	/**
	 * Returns if the cache contains any entries.
	 * @return TRUE if the cache is not empty, otherwise FALSE
	 * @see Collection#isEmpty()
	 */
	public static synchronized boolean isDirty() {
		return (!_cache.isEmpty());
	}

	/**
	 * Clears the position cache.
	 */
	public static void flush() {
		_lastFlush = System.currentTimeMillis();
	}
}