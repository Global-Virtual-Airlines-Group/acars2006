// Copyright 2009, 2012, 2016, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;

import org.deltava.dao.*;

/**
 * A Data Access Object to log ACARS takeoffs and touchdowns. 
 * @author Luke
 * @version 9.0
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
	 * @param isTakeoff TRUE if taking off, otherwise FALSE
	 * @return TRUE if a bounce, otherwise FALSE
	 * @throws DAOException if a JDBC error occurs
	 */
	public boolean logTakeoff(int id, boolean isTakeoff) throws DAOException {
		try {
			startTransaction(); boolean isBounce = false;
			try (PreparedStatement ps = prepareWithoutLimits("UPDATE acars.TOLAND SET EVENT_TIME=NOW() WHERE (ID=?) AND (TAKEOFF=?) LIMIT 1")) {
				ps.setInt(1, id);
				ps.setBoolean(2, isTakeoff);
				isBounce = (executeUpdate(ps, 0) > 0);
			}
			
			// Log if it's not a bounce
			if (!isBounce) {
				try (PreparedStatement ps = prepareWithoutLimits("INSERT INTO acars.TOLAND (ID, TAKEOFF, EVENT_TIME) VALUES (?, ?, NOW())")) {
					ps.setInt(1, id);
					ps.setBoolean(2, isTakeoff);
					executeUpdate(ps, 1);
				}
			}
			
			commitTransaction();
			return isBounce;
		} catch (SQLException se) {
			rollbackTransaction();
			throw new DAOException(se);
		}
	}
}