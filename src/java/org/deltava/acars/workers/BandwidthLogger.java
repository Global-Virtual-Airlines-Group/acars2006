// Copyright 2008, 2009, 2010, 2011, 2016, 2017 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.*;
import java.time.*;
import java.time.temporal.ChronoField;
import java.sql.Connection;

import org.deltava.acars.beans.*;
import org.deltava.beans.acars.*;

import org.deltava.dao.DAOException;
import org.deltava.dao.acars.SetBandwidth;

import org.deltava.util.system.SystemData;

import org.gvagroup.jdbc.ConnectionPool;
import org.gvagroup.ipc.WorkerState;

/**
 * An ACARS Worker to log bandwidth statistics. 
 * @author Luke
 * @version 7.4
 * @since 2.1
 */

public class BandwidthLogger extends Worker {
	
	private ConnectionPool _jdbcPool;
	private final Map<String, ConnectionStats> _lastBW = new HashMap<String, ConnectionStats>();

	/**
	 * Initializes the Worker.
	 */
	public BandwidthLogger() {
		super("Bandwidth Logger", 90, BandwidthLogger.class);
	}
	
	/**
	 * Initializes the Worker.
	 * @see Worker#open()
	 */
	@Override
	public void open() {
		super.open();
		_jdbcPool = (ConnectionPool) SystemData.getObject(SystemData.JDBC_POOL);
	}

	/**
	 * Executes the thread.
	 */
	@Override
	public void run() {
		log.info("Started");
		_status.setStatus(WorkerState.RUNNING);
		while (!Thread.currentThread().isInterrupted()) {
			_status.setMessage("Idle");

			// Wait until we get to the next minute
			ZonedDateTime zdt = ZonedDateTime.now(ZoneOffset.UTC);
			try {
				Thread.sleep((61 - zdt.get(ChronoField.SECOND_OF_MINUTE)) * 1000);
				_status.execute();
				_status.setMessage("Updating statistics");
				
				// Init the counters
				int msgsIn = 0; int msgsOut = 0; int errors = 0; long bytesIn = 0; long bytesOut = 0; long bytesSaved = 0;
				
				// Get the connection statistics
				Collection<String> IDs = new HashSet<String>(_lastBW.keySet());
				Collection<ConnectionStats> stats = _pool.getStatistics();
				for (ConnectionStats ac : stats) {
					ConnectionStats lastBW = _lastBW.get(ac.getID());
					if (lastBW == null)
						lastBW = new ACARSConnectionStats(ac.getID());
					
					// Update totals
					msgsIn += (ac.getMsgsIn() - lastBW.getMsgsIn());
					msgsOut += (ac.getMsgsOut() - lastBW.getMsgsOut());
					bytesIn += (ac.getBytesIn() - lastBW.getBytesIn());
					bytesOut += (ac.getBytesOut() - lastBW.getBytesOut());
					bytesSaved += (ac.getBytesSaved() - lastBW.getBytesSaved());
					errors += (ac.getWriteErrors() - lastBW.getWriteErrors());
					_lastBW.put(ac.getID(), new ACARSConnectionStats(ac));
					IDs.remove(ac.getID());
				}
				
				// Remove the unused entries
				_lastBW.keySet().removeAll(IDs);

				// Init the bean to store period statistics
				Bandwidth bw = new Bandwidth(Instant.now());
				bw.setConnections(stats.size());
				bw.setErrors(errors);
				bw.setMessages(msgsIn, msgsOut);
				bw.setBytes(bytesIn, bytesOut);
				bw.setBytesSaved(bytesSaved);
				
				// Write the bean
				Connection con = null;
				try {
					con = _jdbcPool.getConnection();
					SetBandwidth bwdao = new SetBandwidth(con);
					bwdao.write(bw);
					
					// Do aggregation if we need to
					zdt = ZonedDateTime.ofInstant(bw.getDate(), zdt.getZone());
					if (zdt.get(ChronoField.MINUTE_OF_HOUR) == 0) {
						_status.setMessage("Aggregating data");
						zdt = zdt.minusHours(1).withSecond(0);
						bwdao.aggregate(zdt.toInstant(), 60);
					}
				} catch (DAOException de) {
					log.error(de.getMessage(), de);
				} finally {
					_jdbcPool.release(con);
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