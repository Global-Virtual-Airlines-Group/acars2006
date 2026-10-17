// Copyright 2026 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;

import org.deltava.dao.*;

/**
 * A Data Access Object to write to the AI Summary queue.
 * @author Luke
 * @version 12.5
 * @since 12.5
 */

public class SetSummaryQueue extends DAO {

	/**
	 * Initializes the Data Access Object.
	 * @param c the JDBC connection to use
	 */
	public SetSummaryQueue(Connection c) {
		super(c);
	}

	/**
	 * Writes a flight entry to the queue.
	 * @param flightID the ACARS Flight ID
	 * @param pirepID the Flight Report database ID
	 * @param airlineCode the airline code
	 * @throws DAOException if a JDBC error occurs
	 */
	public void write(int flightID, int pirepID, String airlineCode) throws DAOException {
		try (PreparedStatement ps = prepareWithoutLimits("REPLACE INTO SUMMARY_QUEUE (ID, PIREP_ID, AIRLINE, CREATED) VALUES (?,?,?,NOW())")) {
			ps.setInt(1, flightID);
			ps.setInt(2, pirepID);
			ps.setString(3, airlineCode);
			executeUpdate(ps, 1);
		} catch (SQLException se) {
			throw new DAOException(se);
		}
	}

	/**
	 * Clears an entry from the queue.
	 * @param flightID the ACARS Flight ID
	 * @throws DAOException if a JDBC error occurs
	 */
	public void clear(int flightID) throws DAOException {
		try (PreparedStatement ps = prepareWithoutLimits("DELETE FROM SUMMARY_QUEUE WHERE (ID=?)")) {
			ps.setInt(1, flightID);
			executeUpdate(ps, 0);
		} catch (SQLException se) {
			throw new DAOException(se);
		}
	}
}