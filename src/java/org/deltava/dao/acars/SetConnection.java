// Copyright 2004, 2005, 2008, 2009, 2010, 2011, 2012, 2013, 2015, 2017, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;
import java.util.*;

import org.deltava.dao.*;
import org.deltava.acars.beans.ACARSConnection;

/**
 * A Data Access Object to write ACARS Connection information.
 * @author Luke
 * @version 9.0
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
		if ((c == null) || (c.getUser() == null)) return;
		try (PreparedStatement ps = prepare("INSERT INTO acars.CONS (ID, PILOT_ID, DATE, REMOTE_ADDR, REMOTE_HOST, CLIENT_BUILD, BETA_BUILD) VALUES (CONV(?,10,16), ?, ?, INET6_ATON(?), ?, ?, ?)")) {
			ps.setLong(1, c.getID());
			ps.setInt(2, c.getUser().getID());
			ps.setTimestamp(3, new Timestamp(c.getStartTime()));
			ps.setString(4, c.getRemoteAddr());
			ps.setString(5, c.getRemoteHost());
			ps.setInt(6, c.getClientBuild());
			ps.setInt(7, c.getBeta());
			executeUpdate(ps, 1);
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
		try (PreparedStatement ps = prepareWithoutLimits("UPDATE acars.CONS SET ENDDATE=NOW() WHERE (ID=CONV(?,10,16))")) {
			for (Long id : ids) {
				ps.setLong(1, id.longValue());
				ps.addBatch();
			}
			
			executeUpdate(ps, 0, 0);
		} catch (SQLException se) {
			throw new DAOException(se);
		}
	}
	
	/**
	 * Marks all unclosed connections as closed. This will only close those opened within the past 24 hours.
	 * @return the number of connections marked closed
	 * @throws DAOException if a JDBC error occurs
	 */
	public int closeAll() throws DAOException {
		try (PreparedStatement ps = prepareWithoutLimits("UPDATE acars.CONS SET ENDDATE=NOW() WHERE (ENDDATE IS NULL) AND (DATE>DATE_SUB(NOW(), INTERVAL ? HOUR))")) {
			ps.setInt(1, 24);
			return executeUpdate(ps, 0);
		} catch (SQLException se) {
			throw new DAOException(se);
		}
	}
}