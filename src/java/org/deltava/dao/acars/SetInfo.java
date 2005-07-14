package org.deltava.dao.acars;

import java.sql.*;

import org.deltava.acars.message.InfoMessage;

import org.deltava.dao.DAO;
import org.deltava.dao.DAOException;

/**
 * A Data Access Object to write Flight entries.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public final class SetInfo extends DAO {

	// SQL update statements
	private static final String ISQL = "INSERT INTO acars.FLIGHTS (CON_ID, FLIGHT_NUM, CREATED, EQTYPE, CRUISE_ALT, AIRPORT_D, "
		+ "AIRPORT_A, AIRPORT_L, ROUTE, REMARKS, FSVERSION) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final String USQL = "UPDATE acars.FLIGHTS SET CON_ID=?, FLIGHT_NUM=?, CREATED=?, EQTYPE=?, CRUISE_ALT=?, " +
		"AIRPORT_D=?, AIRPORT_A=?, AIRPORT_L=?, ROUTE=?, REMARKS=?, FSVERSION=? WHERE (ID=?)";
	
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
			_ps.setTimestamp(3, new Timestamp(msg.getTime()));
			_ps.setString(4, msg.getEquipmentType());
			_ps.setString(5, msg.getAltitude());
			_ps.setString(6, msg.getAirportD().getIATA());
			_ps.setString(7, msg.getAirportA().getIATA());
			_ps.setString(8, (msg.getAirportL() == null) ? null : msg.getAirportL().getIATA());
			_ps.setString(9, msg.getAllWaypoints());
			_ps.setString(10, msg.getComments());
			_ps.setInt(11, msg.getFSVersion());
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
	
	public void close(int flightID, long cid) throws DAOException {
	   try {
	      prepareStatement("UPDATE FLIGHTS SET END_TIME=? WHERE (ID=?) AND (CON_ID=?) AND (END_TIME IS NULL)");
	      _ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
	      _ps.setInt(2, flightID);
	      _ps.setLong(3, cid);
	      executeUpdate(1);
	   } catch (SQLException se) {
	      throw new DAOException(se);
	   }
	}
}