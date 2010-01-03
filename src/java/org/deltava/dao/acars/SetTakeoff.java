// Copyright 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;

import org.deltava.dao.*;

/**
 * A Data Access Object to log ACARS takeoffs and touchdowns. 
 * @author Luke
 * @version 2.8
 * @since 2.8
 */

public class SetTakeoff extends DAO {

	/**
	 * Initializes the Data Access Object.
	 * @param c the JDBC connection to use
	 */
	public SetTakeoff(Connection c) {
		super(c);
	}

	/**
	 * Logs a takeoff or landing.
	 * @param id the ACARS flight ID
	 * @param tmsg the TakeoffMessage
	 * @return TRUE if a bounce, otherwise FALSE
	 * @throws DAOException if a JDBC error occurs
	 */
	public boolean logTakeoff(int id, boolean isTakeoff) throws DAOException {
		try {
			prepareStatementWithoutLimits("UPDATE acars.TOLAND SET EVENT_TIME=NOW() WHERE (ID=?) AND (TAKEOFF=?) LIMIT 1");
			_ps.setInt(1, id);
			_ps.setBoolean(2, isTakeoff);
			boolean isBounce = (executeUpdate(0) > 0);
			
			// Log if it's not a bounce
			if (!isBounce) {
				prepareStatementWithoutLimits("INSERT INTO acars.TOLAND (ID, TAKEOFF, EVENT_TIME) VALUES "
						+ "(?, ?, NOW()) ON DUPLICATE KEY UPDATE EVENT_TIME=NOW()");
				_ps.setInt(1, id);
				_ps.setBoolean(2, isTakeoff);
				executeUpdate(1);
			}
			
			return isBounce;
		} catch (SQLException se) {
			throw new DAOException(se);
		}
	}
}