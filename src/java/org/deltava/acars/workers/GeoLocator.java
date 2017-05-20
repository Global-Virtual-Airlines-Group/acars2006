// Copyright 2017 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.*;
import java.sql.Connection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.message.PositionMessage;

import org.deltava.beans.schedule.Country;

import org.deltava.dao.*;

import org.deltava.util.cache.*;
import org.deltava.util.system.SystemData;

import org.gvagroup.ipc.WorkerState;
import org.gvagroup.jdbc.ConnectionPool;

/**
 * An ACARS worker thread to asynchronously geolocate position updates.
 * @author Luke
 * @version 7.4
 * @since 7.4
 */

public class GeoLocator extends Worker {
	
	private ConnectionPool _jdbcPool;
	private GeoCache<CacheableString> _l1c;
	private GeoCache<CacheableString> _l2c;
	
	private final LongAdder _hits = new LongAdder();
	private final LongAdder _reqs = new LongAdder();

	/**
	 * Initializes the Worker.
	 */
	public GeoLocator() {
		super("Geo Locator", 55, GeoLocator.class);
	}
	
	/**
	 * Initializes the Worker.
	 * @see Worker#open()
	 */
	@Override
	public void open() {
		super.open();
		_jdbcPool = (ConnectionPool) SystemData.getObject(SystemData.JDBC_POOL);
		_l1c = CacheManager.getGeo(CacheableString.class, "GeoCountryL1");
		_l2c = CacheManager.getGeo(CacheableString.class, "GeoCountry");
	}

	/**
	 * Executes the thread.
	 */
	@Override
	public void run() {
		log.info("Started");
		_status.setStatus(WorkerState.RUNNING);

		Collection<PositionMessage> upds = new ArrayList<PositionMessage>();
		while (!Thread.currentThread().isInterrupted()) {
			_status.setMessage("Idle - " + _hits.longValue() + " / " + _reqs.longValue());
			
			try {
				Thread.sleep(10000); 			// Sleep for 10s

				// Wait for data
				MessageEnvelope env = GEO_INPUT.poll(20, TimeUnit.SECONDS);
				_status.execute(); _status.setMessage("Loading Position Updates");
				
				// Get all of the updates
				while (env != null) {
					PositionMessage msg = (PositionMessage) env.getMessage();
					_reqs.increment();
					
					// GeoLocate if we can
					CacheableString id = _l1c.get(msg);
					if (id == null)
						id = _l2c.get(msg);
					
					// If not, add it to the queue
					if (id != null) {
						msg.setCountry(Country.get(id.getValue()));
						_hits.increment();
					} else
						upds.add(msg);
					
					env = GEO_INPUT.poll();
				}
				
				// If we have anything left, hit the db
				if (upds.size() > 0) {
					_status.setMessage("Loading Position Updates");
					Connection con = null;
					try {
						con = _jdbcPool.getConnection();
						GetCountry cdao = new GetCountry(con);
						for (PositionMessage msg : upds) {
							Country c = cdao.find(msg); // This will populate L2 automatically
							_l1c.add(msg, new CacheableString("", c.getCode()));
							msg.setCountry(c);
						}
					} catch (DAOException de) {
						log.error(de.getMessage(), de);
					} finally {
						upds.clear();
						_jdbcPool.release(con);
					}
				}
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			
			_status.complete();
		}
	}
}