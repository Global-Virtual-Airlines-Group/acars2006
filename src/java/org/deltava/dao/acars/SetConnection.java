// Copyright 2004, 2005, 2008, 2009, 2010, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;
import java.util.*;

import org.deltava.dao.*;
import org.deltava.acars.beans.ACARSConnection;

/**
 * A Data Access Object to write ACARS Connection information.
 * @author Luke
 * @version 3.6
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
					"CLIENT_BUILD, BETA_BUILD, DISPATCH) VALUES (CONV(?,10,16), ?, ?, INET_ATON(?), ?, ?, ?, ?)");
			_ps.setLong(1, c.getID());
			_ps.setInt(2, c.getUser().getID());
			_ps.setTimestamp(3, new Timestamp(c.getStartTime()));
			_ps.setString(4, c.getRemoteAddr());
			_ps.setString(5, c.getRemoteHost());
			_ps.setInt(6, c.getClientVersion());
			_ps.setInt(7, c.getBeta());
			_ps.setBoolean(8, c.getIsDispatch());
			executeUpdate(1);
		} catch (SQLException se) {
			throw new DAOException(se);
		}
	}
	
	/**
	 * Marks connections as completed.
	 * @param id a Connection ID
	 * @throws DAOException if a JDBC error occurs
	 */
	public void closeConnection(long id) throws DAOException {
		closeConnections(Collections.singleton(Long.valueOf(id)));
	}
	
	/**
	 * Marks connections as completed.
	 * @param ids a Collection of connection IDs
	 * @throws DAOException if a JDBC error occurs
	 */
	public void closeConnections(Collection<Long> ids) throws DAOException {
		try {
			prepareStatementWithoutLimits("UPDATE acars.CONS SET ENDDATE=NOW() WHERE (ID=CONV(?,10,16))");
			for (Iterator<Long> i = ids.iterator(); i.hasNext(); ) {
				long id = i.next().longValue();
				_ps.setLong(1, id);
				_ps.addBatch();
			}
			
			_ps.executeBatch();
			_ps.close();
		} catch (SQLException se) {
			throw new DAOException(se);
		}
	}
}