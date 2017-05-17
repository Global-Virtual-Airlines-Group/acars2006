// Copyright 2017 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.sql.Connection;
import java.util.Collection;
import java.util.concurrent.locks.*;

import org.deltava.acars.message.PositionMessage;
import org.deltava.acars.util.PositionCache;
import org.deltava.beans.schedule.Country;

import org.deltava.dao.*;
import org.deltava.util.cache.*;

import org.deltava.dao.acars.SetPosition;
import org.deltava.jdbc.ConnectionContext;

/**
 * An abstract class for ACARS commands to interact with the Position cache.
 * @author Luke
 * @version 7.3
 * @since 7.3
 */

abstract class PositionCacheCommand extends ACARSCommand {
	
	private static final Lock w = new ReentrantLock();
	private static final PositionCache _posCache = new PositionCache(50, 12500);
	
	private static final GeoCache<CacheableString> _cache = CacheManager.getGeo(CacheableString.class, "GeoCountry");

	/**
	 * Places a PositionMessage in the cache.
	 * @param msg a PositionMessage
	 */
	protected static void queue(PositionMessage msg) {
		_posCache.queue(msg);
	}
	
	/**
	 * Looks
	 * @param msg
	 */
	protected static void lookup(PositionMessage msg) {
		CacheableString id = _cache.get(msg);
		msg.setCountry((id == null) ? null : Country.get(id.getValue()));
	}
	
	/**
	 * Flushes the cache to the database.
	 * @param force TRUE if the cache should be flushed ieven if not full, otherwise FALSE
	 * @return the number of entries written
	 */
	protected static int flush(boolean force, ConnectionContext ctx) throws DAOException {
		if (!w.tryLock())
			return 0;

		boolean doFree = !ctx.hasConnection();
		try {
			if (force || _posCache.isFull()) {
				Collection<PositionMessage> msgs = _posCache.drain(); int entries = msgs.size();
				Connection con = ctx.getConnection();
					
				// Geolocation
				GetCountry cdao = new GetCountry(con);
				for (PositionMessage pmsg : msgs) {
					if (pmsg.getCountry() != null)
						pmsg.setCountry(cdao.find(pmsg));
				}
				
				// Write the entries
				SetPosition dao = new SetPosition(con);
				dao.flush(msgs);
				return entries;
			}
		} finally {
			w.unlock();
			if (doFree)
				ctx.release();
		}
		
		return 0;
	}
}