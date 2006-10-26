// Copyright 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.util;

import java.util.*;

import org.deltava.acars.message.TextMessage;

/**
 * A utility class to cache text messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class TextMessageCache {

	private static final Collection<TextMessageCacheEntry> _cache = new LinkedHashSet<TextMessageCacheEntry>();
	private static long _lastFlush;

	public static class TextMessageCacheEntry {

		private long _conID;
		private int _recipientID;
		private TextMessage _msg;

		TextMessageCacheEntry(TextMessage msg, long conID, int recipientID) {
			super();
			_msg = msg;
			_conID = conID;
			_recipientID = recipientID;
		}

		public TextMessage getMessage() {
			return _msg;
		}

		public long getConnectionID() {
			return _conID;
		}

		public int getRecipientID() {
			return _recipientID;
		}

		public boolean equals(Object o) {
			TextMessageCacheEntry e2 = (TextMessageCacheEntry) o;
			if (_conID != e2._conID)
				return false;

			return (_msg.getID() == e2._msg.getID());
		}
	}

	// Singleton constructor
	private TextMessageCache() {
	}

	/**
	 * Adds a new text message to the cache.
	 * @param msg the Text Message
	 * @param connectionID the ACARS connection ID
	 * @param recipientID the recipient's database ID
	 */
	public static synchronized void push(TextMessage msg, long connectionID, int recipientID) {
		_cache.add(new TextMessageCacheEntry(msg, connectionID, recipientID));
	}

	/**
	 * Returns and removes the first entry out of the cache.
	 * @return the first cache entry, or null
	 */
	public static synchronized TextMessageCacheEntry pop() {
		if (_cache.isEmpty())
			return null;
		
		Iterator<TextMessageCacheEntry> i = _cache.iterator();
		TextMessageCacheEntry entry = i.next();
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
	 * Clears the message cache.
	 */
	public static void flush() {
		_lastFlush = System.currentTimeMillis();
	}
}