/*
 * Created on Mar 9, 2004
 *
 * Write connection info data access object
 */
package org.deltava.dao.acars;

import java.sql.*;

import org.deltava.dao.DAO;
import org.deltava.dao.DAOException;

import org.deltava.acars.beans.ACARSConnection;
import org.deltava.beans.Pilot;

/**
 * A Data Access Object to write ACARS Connection information.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public final class SetConnection extends DAO {
	
	/**
	 * Initializes the Data Access Object.
	 * @param c the JDBC connection to use
	 */
	public SetConnection(Connection c) {
		super(c);
	}
	
	public void add(ACARSConnection c) throws DAOException {
		
		// Get the user information bean from the connection
		Pilot usr = c.getUser();
		if (usr == null)
			throw new DAOException("Connection not Authenticated"); 
		
		try {
			// Init the prepared statement
			prepareStatement("INSERT INTO acars.CONS (ID, PILOT_ID, REMOTE_ADDR, REMOTE_HOST) VALUES (?, ?, ?, ?)");
			
			// Set the prepared statement
			_ps.setLong(1, c.getID());
			_ps.setInt(2, usr.getID());
			_ps.setString(3, c.getRemoteAddr());
			_ps.setString(4, c.getRemoteHost());
			
			// Execute the prepared statement and close
			_ps.executeUpdate();
			_ps.close();
		} catch (SQLException se) {
			throw new DAOException(se.getMessage());
		}
	}
}