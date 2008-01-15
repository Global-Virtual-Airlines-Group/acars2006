// Copyright 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;

import org.deltava.beans.acars.Bandwidth;

import org.deltava.dao.*;

/**
 * A Data Access Object to write ACARS bandwidth statistics.
 * @author Luke
 * @version 2.1
 * @since 2.1
 */

public class SetBandwidth extends DAO {

	/**
	 * Initializes the Data Access Object.
	 * @param c the JDBC connection to use
	 */
	public SetBandwidth(Connection c) {
		super(c);
	}

	/**
	 * Writes a bandwidth entry to the database.
	 * @param bw the Bandwidth bean
	 * @throws DAOException if a JDBC error occurs
	 */
	public void write(Bandwidth bw) throws DAOException {
		try {
			prepareStatement("INSERT INTO acars.BANDWIDTH (PERIOD, DURATION, CONS, BYTES_IN, "
					+ "BYTES_OUT, MSGS_IN, MSGS_OUT) VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
					+ "CONS=GREATEST(CONS,?), BYTES_IN=BYTES_IN+?, BYTES_OUT=BYTES_OUT+?, "
					+ "MSGS_IN=MSGS_IN+?, MSGS_OUT=MSGS_OUT+?");
			_ps.setTimestamp(1, createTimestamp(bw.getDate()));
			_ps.setInt(2, bw.getInterval());
			_ps.setInt(3, bw.getConnections());
			_ps.setLong(4, bw.getBytesIn());
			_ps.setLong(5, bw.getBytesOut());
			_ps.setInt(6, bw.getMsgsIn());
			_ps.setInt(7, bw.getMsgsOut());
			_ps.setInt(8, bw.getConnections());
			_ps.setLong(9, bw.getBytesIn());
			_ps.setLong(10, bw.getBytesOut());
			_ps.setInt(11, bw.getMsgsIn());
			_ps.setInt(12, bw.getMsgsOut());
			executeUpdate(1);
		} catch (SQLException se) {
			throw new DAOException(se);
		}
	}
	
	/**
	 * Aggregates bandwidth statistics for a particular interval.
	 * @param sd the window start date/time
	 * @param interval the window size in minutes
	 * @throws DAOException if a JDBC error occurs
	 */
	public void aggregate(java.util.Date sd, int interval) throws DAOException {
		try {
			startTransaction();
			
			// Aggregate the data
			prepareStatement("REPLACE INTO acars.BANDWIDTH (PERIOD, DURATION, CONS, BYTES_IN, "
					+ "BYTES_OUT, MSGS_IN, MSGS_OUT, PEAK_CONS, PEAK_BYTES, PEAK_MSGS) SELECT "
					+ "DATE_SUB(PERIOD, INTERVAL MINUTE(PERIOD) MINUTE) AS HR, ?, AVG(CONS), "
					+ "SUM(BYTES_IN), SUM(BYTES_OUT), SUM(MSGS_IN), SUM(MSGS_OUT), MAX(CONS), "
					+ "MAX(BYTES_IN+BYTES_OUT), MAX(MSGS_IN+MSGS_OUT) FROM acars.BANDWIDTH "
					+ "WHERE (DURATION=?) AND (PERIOD >= ?) AND "
					+ "(PERIOD < DATE_ADD(?, INTERVAL ? MINUTE)) GROUP BY HR");
			_ps.setTimestamp(1, createTimestamp(sd));
			_ps.setInt(2, interval);
			_ps.setInt(3, 1);
			_ps.setTimestamp(4, createTimestamp(sd));
			_ps.setTimestamp(5, createTimestamp(sd));
			_ps.setInt(5, interval);
			executeUpdate(0);
			
			// Clear out the minute by minute intervals
			prepareStatementWithoutLimits("DELETE FROM acars.BANDWIDTH WHERE (DURATION=?) AND "
					+ "(PERIOD >= ?) AND (PERIOD < DATE_ADD(?, INTERVAL ? MINUTE))");
			_ps.setInt(1, 1);
			_ps.setTimestamp(2, createTimestamp(sd));
			_ps.setTimestamp(3, createTimestamp(sd));
			executeUpdate(1);
			
			// Commit
			commitTransaction();
		} catch (SQLException se) {
			rollbackTransaction();
			throw new DAOException(se);
		}
	}
}