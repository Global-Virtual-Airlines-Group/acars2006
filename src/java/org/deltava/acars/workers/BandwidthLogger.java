// Copyright 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.*;
import java.sql.Connection;

import org.deltava.acars.beans.*;
import org.deltava.beans.acars.Bandwidth;

import org.deltava.dao.DAOException;
import org.deltava.dao.acars.SetBandwidth;

import org.deltava.jdbc.ConnectionPool;
import org.deltava.util.system.SystemData;

/**
 * An ACARS Worker to log bandwidth statistics. 
 * @author Luke
 * @version 2.1
 * @since 2.1
 */

public class BandwidthLogger extends Worker {
	
	private ConnectionPool _jdbcPool;
	private Bandwidth _lastBW;

	/**
	 * Initializes the Worker.
	 */
	public BandwidthLogger() {
		super("Bandwidth Logger", BandwidthLogger.class);
	}
	
	/**
	 * Initializes the Worker.
	 * @see Worker#open()
	 */
	public void open() {
		super.open();
		_lastBW = new Bandwidth(new Date());
		
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
				_status.setMessage("Updating statistics");
				
				// Init the counters
				int msgsIn = 0; int msgsOut = 0;
				long bytesIn = 0; long bytesOut = 0;
				
				// Get the connection statistics
				Collection<ACARSConnection> cons = new ArrayList<ACARSConnection>(_pool.get("*"));
				for (Iterator<ACARSConnection> i = cons.iterator(); i.hasNext(); ) {
					ACARSConnection ac = i.next();
					msgsIn += ac.getMsgsIn();
					msgsOut += ac.getMsgsOut();
					bytesIn += ac.getBytesIn();
					bytesOut += ac.getBytesOut();
				}

				// Init the bean
				Bandwidth bw = new Bandwidth(new Date());
				bw.setConnections(cons.size());
				bw.setMessages(msgsIn - _lastBW.getMsgsIn(), msgsOut - _lastBW.getMsgsOut());
				bw.setBytes(bytesIn - _lastBW.getBytesIn(), bytesOut - _lastBW.getBytesOut());
				
				// Write the bean
				Connection con = null;
				try {
					con = _jdbcPool.getConnection(true);
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
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}
}