// Copyright 2017, 2023, 2024 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.*;
import java.sql.Connection;
import java.util.concurrent.TimeUnit;

import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.message.PositionMessage;
import org.deltava.beans.schedule.Country;

import org.deltava.dao.*;

import org.deltava.util.cache.*;
import org.deltava.util.system.SystemData;

import org.gvagroup.ipc.WorkerState;
import org.gvagroup.pool.ConnectionPool;

/**
 * An ACARS worker thread to asynchronously geolocate position updates.
 * @author Luke
 * @version 11.3
 * @since 7.4
 */

public class GeoLocator extends Worker {
	
	private ConnectionPool<Connection> _jdbcPool;
	private GeoCache<CacheableString> _l1c;
	private GeoCache<CacheableString> _l2c;
	
	private long _hits = 0;
	private long _reqs = 0;

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
		_jdbcPool = SystemData.getJDBCPool();
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

		Collection<PositionMessage> upds = new ArrayList<PositionMessage>(); long startTime = 0, execTime = 0;
		while (!Thread.currentThread().isInterrupted()) {
			_status.setMessage("Idle - " + _hits + " hits / " + _reqs + " reqs");
			
			try {
				Thread.sleep(10000 - execTime); 			// Sleep for 10s

				// Wait for data
				MessageEnvelope env = GEO_INPUT.poll(20, TimeUnit.SECONDS);
				startTime = System.currentTimeMillis();
				_status.execute(); _status.setMessage("Loading Position Updates");
				
				// Get all of the updates
				while (env != null) {
					PositionMessage msg = (PositionMessage) env.getMessage();
					_reqs++;
					
					// GeoLocate if we can
					CacheableString id = _l1c.get(msg);
					if (id == null) {
						id = _l2c.get(msg);
						if (id != null)
							_l1c.add(msg, id);
					}
					
					// If not, add it to the queue
					if (id != null) {
						msg.setCountry(Country.get(id.getValue()));
						_hits++;
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
							Country c = cdao.find(msg, true); // This will populate L2 automatically
							_l1c.add(msg, new CacheableString("", c.getCode()));
							msg.setCountry(c);
						}
					} catch (DAOException de) {
						log.atError().withThrowable(de).log(de.getMessage());
					} finally {
						upds.clear();
						_jdbcPool.release(con);
					}
				}
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				log.atError().withThrowable(e).log(e.getMessage());
			} finally {
				execTime = Math.min(9750, System.currentTimeMillis() - startTime);
				_status.complete();
			}
		}
	}
}