// Copyright 2010, 2011, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;

import org.deltava.dao.*;

/**
 * A Data Access Object to log User warnings.
 * @author Luke
 * @version 9.0
 * @since 4.0
 */

public class SetWarning extends DAO {

	/**
	 * Initializes the Data Access Object.
	 * @param c the JDBC connection to use
	 */
	public SetWarning(Connection c) {
		super(c);
	}

	/**
	 * Logs a warning.
	 * @param userID the user's database ID
	 * @param authorID the author's database ID
	 * @param score the warning points
	 * @throws DAOException if a JDBC error occurs
	 */
	public void warn(int userID, int authorID, int score) throws DAOException {
		try (PreparedStatement ps = prepare("REPLACE INTO acars.WARNINGS (ID, AUTHOR, WARNED_ON, SCORE) VALUES (?, ?, NOW(), ?)")) {
			ps.setInt(1, userID);
			ps.setInt(2, authorID);
			ps.setInt(3, score);
			executeUpdate(ps, 1);
		} catch (SQLException se) {
			throw new DAOException(se);
		}
	}
	
	/**
	 * Clears a user's warnings.
	 * @param userID the user's database ID
	 * @throws DAOException if a JDBC error occurs
	 */
	public void clear(int userID) throws DAOException {
		try (PreparedStatement ps = prepareWithoutLimits("DELETE FROM acars.WARNINGS WHERE (ID=?)")) {
			ps.setInt(1, userID);
			executeUpdate(ps, 0);
		} catch (SQLException se) {
			throw new DAOException(se);
		}		
	}
	
	/**
	 * Purges warnings older than a certain date.
	 * @param days the number of days
	 * @throws DAOException if a JDBC error occurs
	 */
	public void purge(int days) throws DAOException {
		try (PreparedStatement ps = prepareWithoutLimits("DELETE FROM acars.WARNINGS WHERE (WARNED_ON < DATE_SUB(NOW(), INTERVAL ? DAYS))")) {
			ps.setInt(1, days);
			executeUpdate(ps, 0);
		} catch (SQLException se) {
			throw new DAOException(se);
		}
	}
}