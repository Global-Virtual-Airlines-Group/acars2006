// Copyright 2008, 2009, 2010, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.*;
import java.sql.Connection;

import org.deltava.acars.beans.*;
import org.deltava.beans.acars.*;

import org.deltava.dao.DAOException;
import org.deltava.dao.acars.SetBandwidth;

import org.deltava.util.system.SystemData;

import org.gvagroup.jdbc.ConnectionPool;
import org.gvagroup.ipc.WorkerStatus;

/**
 * An ACARS Worker to log bandwidth statistics. 
 * @author Luke
 * @version 4.0
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
	public void open() {
		super.open();
		
		// Get the connection pool
		_jdbcPool = (ConnectionPool) SystemData.getObject(SystemData.JDBC_POOL);
	}

	/**
	 * Executes the thread.
	 */
	public void run() {
		log.info("Started");
		_status.setStatus(WorkerStatus.STATUS_START);
		while (!Thread.currentThread().isInterrupted()) {
			_status.setMessage("Idle");

			// Wait until we get to the next minute
			Calendar cld = Calendar.getInstance();
			try {
				Thread.sleep((61 - cld.get(Calendar.SECOND)) * 1000);
				_status.execute();
				_status.setMessage("Updating statistics");
				
				// Init the counters
				int msgsIn = 0; int msgsOut = 0; int errors = 0;
				long bytesIn = 0; long bytesOut = 0;
				
				// Get the connection statistics
				Collection<String> IDs = new HashSet<String>(_lastBW.keySet());
				Collection<ConnectionStats> stats = _pool.getStatistics();
				for (Iterator<ConnectionStats> i = stats.iterator(); i.hasNext(); ) {
					ConnectionStats ac = i.next();
					ConnectionStats lastBW = _lastBW.get(ac.getID());
					if (lastBW == null)
						lastBW = new ACARSConnectionStats(ac.getID());
					
					// Update totals
					msgsIn += (ac.getMsgsIn() - lastBW.getMsgsIn());
					msgsOut += (ac.getMsgsOut() - lastBW.getMsgsOut());
					bytesIn += (ac.getBytesIn() - lastBW.getBytesIn());
					bytesOut += (ac.getBytesOut() - lastBW.getBytesOut());
					errors += (ac.getWriteErrors() - lastBW.getWriteErrors());
					_lastBW.put(ac.getID(), new ACARSConnectionStats(ac));
					IDs.remove(ac.getID());
				}
				
				// Remove the unused entries
				_lastBW.keySet().removeAll(IDs);

				// Init the bean to store period statistics
				Bandwidth bw = new Bandwidth(new Date());
				bw.setConnections(stats.size());
				bw.setErrors(errors);
				bw.setMessages(msgsIn, msgsOut);
				bw.setBytes(bytesIn, bytesOut);
				
				// Write the bean
				Connection con = null;
				try {
					con = _jdbcPool.getConnection();
					SetBandwidth bwdao = new SetBandwidth(con);
					bwdao.write(bw);
					
					// Do aggregation if we need to
					cld.setTime(bw.getDate());
					if (cld.get(Calendar.MINUTE) == 0) {
						_status.setMessage("Aggregating data");
						cld.add(Calendar.HOUR_OF_DAY, -1);
						bwdao.aggregate(cld.getTime(), 60);
					}
				} catch (DAOException de) {
					log.error(de.getMessage(), de);
				} finally {
					_jdbcPool.release(con);
				}
				
				_status.complete();
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}
}