package org.deltava.dao.acars;

import java.sql.*;

import org.deltava.acars.message.InfoMessage;

import org.deltava.dao.DAO;
import org.deltava.dao.DAOException;

/**
 * A Data Access Object to write Flight Information entries.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public final class SetInfo extends DAO {

	// SQL update statements
	private static final String ISQL = "INSERT INTO acars.FLIGHTS (CON_ID, FLIGHT_NUM, CREATED, EQTYPE, CRUISE_ALT, AIRPORT_D, "
		+ "AIRPORT_A, ROUTE, REMARKS, FSVERSION, OFFLINE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final String USQL = "UPDATE acars.FLIGHTS SET CON_ID=?, FLIGHT_NUM=?, CREATED=?, EQTYPE=?, CRUISE_ALT=?, "
		+ "AIRPORT_D=?, AIRPORT_A=?, ROUTE=?, REMARKS=?, FSVERSION=?, OFFLINE=?, END_TIME=NULL WHERE (ID=?)";
	
	/**
	 * Initialize the Data Access Object.
	 * @param c the JDBC connection to use
	 */
	public SetInfo(Connection c) {
		super(c);
	}
	
	public void write(InfoMessage msg, long cid) throws DAOException {
		try {
			prepareStatement((msg.getFlightID() == 0) ? ISQL : USQL);
			
			// Set the prepared statement
			_ps.setLong(1, cid);
			_ps.setString(2, msg.getFlightCode());
			_ps.setTimestamp(3, createTimestamp(msg.getStartTime()));
			_ps.setString(4, msg.getEquipmentType());
			_ps.setString(5, msg.getAltitude());
			_ps.setString(6, msg.getAirportD().getIATA());
			_ps.setString(7, msg.getAirportA().getIATA());
			_ps.setString(8, msg.getAllWaypoints());
			_ps.setString(9, msg.getComments());
			_ps.setInt(10, msg.getFSVersion());
			_ps.setBoolean(11, msg.isOffline());
			if (msg.getFlightID() != 0)
				_ps.setInt(12, msg.getFlightID());
			
			// Write to the database and close the statement
			_ps.executeUpdate();
			_ps.close();
			
			// If we're writing a new entry, get the database ID
			if (msg.getFlightID() == 0)
				msg.setFlightID(getNewID());
		} catch (SQLException se) {
			throw new DAOException(se.getMessage());
		}
	}
	
	/**
	 * Marks a Flight as complete.
	 * @param flightID the Flight ID
	 * @param cid the ACARS Connection ID
	 * @param force mark the flight closed even if already closed
	 * @throws DAOException if a JDBC error occurs
	 */
	public void close(int flightID, long cid, boolean force) throws DAOException {
	   
	   // Build the SQL statement
	   StringBuffer sqlBuf = new StringBuffer("UPDATE acars.FLIGHTS SET END_TIME=NOW() WHERE (ID=?) AND (CON_ID=?)");
	   if (!force)
	      sqlBuf.append(" AND (END_TIME IS NULL)");
	   
	   try {
	      prepareStatement(sqlBuf.toString());
	      _ps.setInt(1, flightID);
	      _ps.setLong(2, cid);
	      executeUpdate(force ? 1 : 0);
	   } catch (SQLException se) {
	      throw new DAOException(se);
	   }
	}
	
	/**
	 * Marks a flight as having a filed PIREP, since the PIREP can be in different databases.
	 * @param flightID the Flight ID
	 * @throws DAOException if a JDBC error occurs
	 */
	public void logPIREP(int flightID) throws DAOException {
	   try {
	      prepareStatement("UPDATE acars.FLIGHTS SET PIREP=? WHERE (ID=?)");
	      _ps.setBoolean(1, true);
	      _ps.setInt(2, flightID);
	      executeUpdate(0);
	   } catch (SQLException se) {
	      throw new DAOException(se);
	   }
	}
}