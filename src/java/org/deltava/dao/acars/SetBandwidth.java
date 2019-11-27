// Copyright 2008, 2010, 2016, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;

import org.deltava.beans.acars.Bandwidth;

import org.deltava.dao.*;

/**
 * A Data Access Object to write ACARS bandwidth statistics.
 * @author Luke
 * @version 9.0
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
		try (PreparedStatement ps = prepare("INSERT INTO acars.BANDWIDTH (PERIOD, DURATION, CONS, BYTES_IN, BYTES_OUT, MSGS_IN, MSGS_OUT, ERRORS, BYTES_SAVED) VALUES "
			+ "(?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE CONS=GREATEST(CONS,?), BYTES_IN=BYTES_IN+?, BYTES_OUT=BYTES_OUT+?, MSGS_IN=MSGS_IN+?, MSGS_OUT=MSGS_OUT+?, "
			+ "ERRORS=ERRORS+?, BYTES_SAVED=BYTES_SAVED+?")) {
			ps.setTimestamp(1, createTimestamp(bw.getDate()));
			ps.setInt(2, bw.getInterval());
			ps.setInt(3, bw.getConnections());
			ps.setLong(4, bw.getBytesIn());
			ps.setLong(5, bw.getBytesOut());
			ps.setInt(6, bw.getMsgsIn());
			ps.setInt(7, bw.getMsgsOut());
			ps.setInt(8, bw.getErrors());
			ps.setLong(9, bw.getBytesSaved());
			ps.setInt(10, bw.getConnections());
			ps.setLong(11, bw.getBytesIn());
			ps.setLong(12, bw.getBytesOut());
			ps.setInt(13, bw.getMsgsIn());
			ps.setInt(14, bw.getMsgsOut());
			ps.setInt(15, bw.getErrors());
			ps.setLong(16, bw.getBytesSaved());
			executeUpdate(ps, 1);
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
	public void aggregate(java.time.Instant sd, int interval) throws DAOException {
		try {
			startTransaction();
			
			// Aggregate the data
			try (PreparedStatement ps = prepare("REPLACE INTO acars.BANDWIDTH (PERIOD, DURATION, CONS, BYTES_IN, BYTES_OUT, MSGS_IN, MSGS_OUT, PEAK_CONS, PEAK_BYTES, PEAK_MSGS, "
				+ "ERRORS, BYTES_SAVED) SELECT DATE_SUB(PERIOD, INTERVAL MINUTE(PERIOD) MINUTE) AS HR, ?, AVG(CONS), SUM(BYTES_IN), SUM(BYTES_OUT), SUM(MSGS_IN), SUM(MSGS_OUT), "
				+ "MAX(CONS), MAX(BYTES_IN+BYTES_OUT), MAX(MSGS_IN+MSGS_OUT), SUM(ERRORS), SUM(BYTES_SAVED) FROM acars.BANDWIDTH WHERE (DURATION=?) AND (PERIOD >= ?) AND "
				+ "(PERIOD < DATE_ADD(?, INTERVAL ? MINUTE)) GROUP BY HR")) {
				ps.setInt(1, interval);
				ps.setInt(2, 1);
				ps.setTimestamp(3, createTimestamp(sd));
				ps.setTimestamp(4, createTimestamp(sd));
				ps.setInt(5, interval);
				executeUpdate(ps, 0);
			}
			
			// Clear out the minute by minute intervals
			try (PreparedStatement ps = prepareWithoutLimits("DELETE FROM acars.BANDWIDTH WHERE (DURATION=?) AND (PERIOD >= ?) AND (PERIOD < DATE_ADD(?, INTERVAL ? MINUTE))")) {
				ps.setInt(1, 1);
				ps.setTimestamp(2, createTimestamp(sd));
				ps.setTimestamp(3, createTimestamp(sd));
				ps.setInt(4, interval);
				executeUpdate(ps, 1);
			}
			
			commitTransaction();
		} catch (SQLException se) {
			rollbackTransaction();
			throw new DAOException(se);
		}
	}
}