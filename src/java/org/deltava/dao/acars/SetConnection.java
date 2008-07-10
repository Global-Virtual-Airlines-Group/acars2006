// Copyright 2004, 2005, 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;

import org.deltava.dao.*;
import org.deltava.acars.beans.ACARSConnection;

/**
 * A Data Access Object to write ACARS Connection information.
 * @author Luke
 * @version 2.2
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
	
	/**
	 * Logs an ACARS Connection record to the database.
	 * @param c the ACARSConnection bean
	 * @throws DAOException if a JDBC error occurs
	 */
	public void add(ACARSConnection c) throws DAOException {
		if ((c == null) || (c.getUser() == null))
			return;
		
		try {
			prepareStatement("INSERT INTO acars.CONS (ID, PILOT_ID, DATE, REMOTE_ADDR, REMOTE_HOST, " +
					"CLIENT_BUILD, BETA_BUILD, MP) VALUES (?, ?, ?, INET_ATON(?), ?, ?, ?, ?)");
			
			// Set the prepared statement
			_ps.setLong(1, c.getID());
			_ps.setInt(2, c.getUser().getID());
			_ps.setTimestamp(3, new Timestamp(c.getStartTime()));
			_ps.setString(4, c.getRemoteAddr());
			_ps.setString(5, c.getRemoteHost());
			_ps.setInt(6, c.getClientVersion());
			_ps.setInt(7, c.getBeta());
			_ps.setBoolean(8, false);
			executeUpdate(1);
		} catch (SQLException se) {
			throw new DAOException(se);
		}
	}
	
	/**
	 * Marks an ACARS Connection as using multi-player.
	 * @param id the Connection ID
	 * @throws DAOException if a JDBC error occurs
	 */
	public void toggleMP(long id) throws DAOException {
		try {
			prepareStatement("UPDATE acars.CONS SET MP=? WHERE (ID=?)");
			_ps.setBoolean(1, true);
			executeUpdate(1);
		} catch (SQLException se) {
			throw new DAOException(se);
		}
	}
}