// Copyright 2017 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.sql.Connection;
import java.util.Collection;
import java.util.concurrent.locks.*;

import org.deltava.acars.message.PositionMessage;
import org.deltava.acars.util.PositionCache;

import org.deltava.dao.*;
import org.deltava.dao.acars.SetPosition;
import org.deltava.jdbc.ConnectionContext;

/**
 * 
 * @author Luke
 * @version 7.3
 * @since 7.3
 */

abstract class PositionCacheCommand extends ACARSCommand {

	private static final Lock w = new ReentrantLock();
	private static final PositionCache _cache = new PositionCache(50, 15200);

	/**
	 * Places a PositionMessage in the cache.
	 * @param msg a PositionMessage
	 */
	protected static void queue(PositionMessage msg) {
		_cache.queue(msg);
	}
	
	/**
	 * 
	 * @param force
	 * @return the number of entries written
	 */
	protected static int flush(boolean force, ConnectionContext ctx) throws DAOException {
		if (!w.tryLock())
			return 0;

		try {
			if (force || _cache.isFull()) {
				Collection<PositionMessage> msgs = _cache.drain(); int entries = msgs.size();
				Connection con = ctx.getConnection();
					
				// Geolocation
				GetCountry cdao = new GetCountry(con);
				for (PositionMessage pmsg : msgs)
					pmsg.setCountry(cdao.find(pmsg));
				
				// Write the entries
				SetPosition dao = new SetPosition(con);
				dao.flush(msgs);
				return entries;
			}
		} finally {
			ctx.release();
			w.unlock();
		}
		
		return 0;
	}
}