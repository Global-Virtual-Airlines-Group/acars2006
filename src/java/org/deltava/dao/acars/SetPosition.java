// Copyright 2004, 2005, 2006, 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;
import java.util.Calendar;

import org.deltava.dao.*;

import org.deltava.acars.message.PositionMessage;

import org.deltava.util.CalendarUtils;

/**
 * A Data Access Object to write ACARS Position Messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public final class SetPosition extends DAO {

	/**
	 * Initializes the Data Access Object.
	 * @param c the JDBC connection to use
	 */
	public SetPosition(Connection c) {
		super(c);
	}
	
	/**
	 * Writes a position message to the database.
	 * @param msg the Position data
	 * @param cid the connection ID
	 * @param flightID the flight ID
	 * @throws DAOException if a JDBC error occurs
	 */
	public void write(PositionMessage msg, long cid, int flightID) throws DAOException {
		
		// Build the timestamp
		Calendar cld = CalendarUtils.getInstance(msg.getDate());
		int ms = cld.get(Calendar.MILLISECOND);
		
		try {
			prepareStatementWithoutLimits("INSERT INTO acars.POSITIONS (CON_ID, FLIGHT_ID, REPORT_TIME, TIME_MS, LAT, LNG, B_ALT, "
					+ "R_ALT, HEADING, ASPEED, GSPEED, VSPEED, N1, N2, MACH, FUEL, PHASE, SIM_RATE, FLAGS, FLAPS, PITCH, BANK, "
					+ "FUELFLOW, WIND_HDG, WIND_SPEED, AOA, GFORCE, FRAMERATE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
					+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			
			// Set the prepared statement parameters
			_ps.setLong(1, cid);
			_ps.setInt(2, flightID);
			_ps.setTimestamp(3, new Timestamp(cld.getTimeInMillis() - ms));
			_ps.setInt(4, ms);
			_ps.setDouble(5, msg.getLatitude());
			_ps.setDouble(6, msg.getLongitude());
			_ps.setInt(7, msg.getAltitude());
			_ps.setInt(8, msg.getRadarAltitude());
			_ps.setInt(9, msg.getHeading());
			_ps.setInt(10, msg.getAspeed());
			_ps.setInt(11, msg.getGspeed());
			_ps.setInt(12, msg.getVspeed());
			_ps.setDouble(13, msg.getN1());
			_ps.setDouble(14, msg.getN2());
			_ps.setDouble(15, msg.getMach());
			_ps.setInt(16, msg.getFuelRemaining());
			_ps.setInt(17, msg.getPhase());
			_ps.setInt(18, msg.getSimRate());
			_ps.setInt(19, msg.getFlags());
			_ps.setInt(20, msg.getFlaps());
			_ps.setDouble(21, msg.getPitch());
			_ps.setDouble(22, msg.getBank());
			_ps.setInt(23, msg.getFuelFlow());
			_ps.setInt(24, msg.getWindHeading());
			_ps.setInt(25, msg.getWindSpeed());
			_ps.setDouble(26, msg.getAngleOfAttack());
			_ps.setDouble(27, msg.getG());
			_ps.setInt(28, msg.getFrameRate());
			executeUpdate(1);
		} catch (SQLException se) {
			throw new DAOException(se);
		}
	}
}