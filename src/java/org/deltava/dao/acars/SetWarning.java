// Copyright 2010, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;

import org.deltava.dao.*;

/**
 * A Data Access Object to log User warnings.
 * @author Luke
 * @version 4.0
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
	 * @throws DAOException if a JDBC error occurs
	 */
	public void warn(int userID, int authorID) throws DAOException {
		try {
			prepareStatement("REPLACE INTO acars.WARNINGS (ID, AUTHOR, WARNED_ON) VALUES (?, ?, NOW())");
			_ps.setInt(1, userID);
			_ps.setInt(2, authorID);
			executeUpdate(1);
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
		try {
			prepareStatementWithoutLimits("DELETE FROM acars.WARNINGS WHERE (ID=?)");
			_ps.setInt(1, userID);
			executeUpdate(0);
		} catch (SQLException se) {
			throw new DAOException(se);
		}		
	}
}