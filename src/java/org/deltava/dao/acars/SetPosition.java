package org.deltava.dao.acars;

import java.sql.*;

import org.deltava.dao.DAO;
import org.deltava.dao.DAOException;

import org.deltava.acars.message.PositionMessage;

/**
 * A Data Access Object to write ACARS Position Messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public final class SetPosition extends DAO {

	// SQL prepared statement syntax
	private static final String SQL = 	"INSERT INTO acars.POSITIONS (CON_ID, FLIGHT_ID, REPORT_TIME, LAT, LNG, B_ALT, R_ALT, "
		+ "HEADING, ASPEED, GSPEED, VSPEED, N1, N2, MACH, FUEL, PHASE, SIM_RATE, FLAGS, FLAPS) VALUES " 
		+ "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	/**
	 * Initializes the Data Access Object.
	 * @param c the JDBC connection to use
	 */
	public SetPosition(Connection c) {
		super(c);
	}
	
	/**
	 * Helper method to initialize the prepared statement.
	 */
	private void initStatement() throws SQLException {
	   if (_ps == null)
	      prepareStatementWithoutLimits(SQL);
	   else
	      _ps.clearParameters();
	}
	
	/**
	 * Writes a position message to the database.
	 * @param msg the Position data
	 * @param cid the connection ID
	 * @param flightID the flight ID
	 * @throws DAOException if a JDBC error occurs
	 */
	public void write(PositionMessage msg, long cid, int flightID) throws DAOException {
		try {
			initStatement();
			
			// Set the prepared statement parameters
			_ps.setLong(1, cid);
			_ps.setInt(2, flightID);
			_ps.setTimestamp(3, createTimestamp(msg.getDate()));
			_ps.setDouble(4, msg.getLatitude());
			_ps.setDouble(5, msg.getLongitude());
			_ps.setInt(6, msg.getAltitude());
			_ps.setInt(7, msg.getRadarAltitude());
			_ps.setInt(8, msg.getHeading());
			_ps.setInt(9, msg.getAspeed());
			_ps.setInt(10, msg.getGspeed());
			_ps.setInt(11, msg.getVspeed());
			_ps.setDouble(12, msg.getN1());
			_ps.setDouble(13, msg.getN2());
			_ps.setDouble(14, msg.getMach());
			_ps.setInt(15, msg.getFuelRemaining());
			_ps.setInt(16, msg.getPhase());
			_ps.setInt(17, msg.getSimRate());
			_ps.setInt(18, msg.getFlags());
			_ps.setInt(19, msg.getFlaps());
			
			// execute the statement
			_ps.executeUpdate();
		} catch (SQLException se) {
			throw new DAOException(se);
		}
	}

	/**
	 * Releases the prepared statement.
	 */
	public void release() {
	   try {
	      _ps.close();
	   } catch (Exception e) {
	   }
	}
}