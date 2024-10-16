// Copyright 2017, 2020, 2024 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;
import java.sql.Connection;
import java.util.concurrent.locks.*;

import org.deltava.acars.message.PositionMessage;
import org.deltava.acars.util.PositionCache;

import org.deltava.beans.acars.TrackUpdate;
import org.deltava.beans.schedule.Country;

import org.deltava.dao.*;
import org.deltava.util.cache.*;

import org.deltava.dao.acars.SetPosition;
import org.deltava.dao.redis.SetTrack;

import org.deltava.jdbc.ConnectionContext;

/**
 * An abstract class for ACARS commands to interact with the Position caches.
 * @author Luke
 * @version 11.3
 * @since 7.3
 */

abstract class PositionCacheCommand extends ACARSCommand {
	
	private static final Lock w = new ReentrantLock();
	private static final PositionCache<PositionMessage> _posCache = new PositionCache<PositionMessage>(50, 12500);
	private static final PositionCache<TrackUpdate> _trkCache = new PositionCache<TrackUpdate>(10, 15000);
	
	private static final GeoCache<CacheableString> _geoCache = CacheManager.getGeo(CacheableString.class, "GeoCountry");

	/**
	 * Places a PositionMessage in the position cache.
	 * @param msg a PositionMessage
	 */
	protected static void queue(PositionMessage msg) {
		_posCache.queue(msg);
	}
	
	/**
	 * Places a TrackUpdate in the track cache. 
	 * @param upd a TrackUpdate
	 */
	protected static void queue(TrackUpdate upd) {
		_trkCache.queue(upd);
	}
	
	/**
	 * Performs a position lookup.
	 * @param msg the PositionMessage
	 */
	protected static void lookup(PositionMessage msg) {
		CacheableString id = _geoCache.get(msg);
		if (id != null)
			msg.setCountry(Country.get(id.getValue()));
	}
	
	/**
	 * Flushes the cache to the database.
	 * @param force TRUE if the cache should be flushed ieven if not full, otherwise FALSE
	 * @return the number of entries written
	 */
	protected static int flush(boolean force, ConnectionContext ctx) throws DAOException {
		if (!w.tryLock())
			return 0;
		
		// Write track updates
		if (force || _trkCache.isFull()) {
			SetTrack tdao = new SetTrack();
			Collection<TrackUpdate> upds = _trkCache.drain();
			tdao.write(upds);
		}

		boolean doFree = !ctx.hasConnection();
		try {
			if (force || _posCache.isFull()) {
				Connection con = ctx.getConnection();
				Collection<PositionMessage> msgs = _posCache.drain(); int entries = msgs.size(); // This prevents a drain if the pool is full
					
				// Geolocation
				GetCountry cdao = new GetCountry(con);
				for (PositionMessage pmsg : msgs) {
					if (pmsg.getCountry() == null)
						pmsg.setCountry(cdao.find(pmsg, true));
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