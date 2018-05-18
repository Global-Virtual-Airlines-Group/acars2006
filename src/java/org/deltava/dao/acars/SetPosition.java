// Copyright 2004, 2005, 2006, 2007, 2010, 2012, 2014, 2016, 2017, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;
import java.util.*;

import org.deltava.dao.*;

import org.deltava.acars.message.PositionMessage;

/**
 * A Data Access Object to write ACARS Position Messages.
 * @author Luke
 * @version 8.3
 * @since 1.0
 */

public class SetPosition extends DAO {
	
	/**
	 * Initializes the Data Access Object.
	 * @param c the JDBC connection to use
	 */
	public SetPosition(Connection c) {
		super(c);
	}
	
	/**
	 * Flushes the queue to the database.
	 * @param entries the entries to write
	 * @throws DAOException if a JDBC error occurs
	 */
	public void flush(Collection<PositionMessage> entries) throws DAOException {
		try {
			prepareStatementWithoutLimits("REPLACE INTO acars.POSITIONS (FLIGHT_ID, REPORT_TIME, SIM_TIME, LAT, LNG, B_ALT, R_ALT, HEADING, ASPEED, GSPEED, VSPEED, "
				+ "N1, N2, MACH, FUEL, PHASE, SIM_RATE, FLAGS, FLAPS, PITCH, BANK, FUELFLOW, WIND_HDG, WIND_SPEED, TEMP, PRESSURE, VIZ, AOA, GFORCE, FRAMERATE, "
				+ "NAV1, NAV2, VAS, WEIGHT, ASTYPE, ADF1) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			
			// Write core entries
			startTransaction();
			for (Iterator<PositionMessage> i = entries.iterator(); i.hasNext(); ) {
				PositionMessage msg = i.next();
				
				// Set the prepared statement parameters
				_ps.setInt(1, msg.getFlightID());
				_ps.setTimestamp(2, createTimestamp(msg.getDate()));
				_ps.setTimestamp(3, createTimestamp(msg.getSimTime()));
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
				_ps.setInt(16, msg.getPhase().ordinal());
				_ps.setInt(17, msg.getSimRate());
				_ps.setInt(18, msg.getFlags());
				_ps.setInt(19, msg.getFlaps());
				_ps.setDouble(20, msg.getPitch());
				_ps.setDouble(21, msg.getBank());
				_ps.setInt(22, msg.getFuelFlow());
				_ps.setInt(23, msg.getWindHeading());
				_ps.setInt(24, msg.getWindSpeed());
				_ps.setInt(25, msg.getTemperature());
				_ps.setInt(26, msg.getPressure());
				_ps.setDouble(27, msg.getVisibility());
				_ps.setDouble(28, msg.getAngleOfAttack());
				_ps.setDouble(29, msg.getG());
				_ps.setInt(30, msg.getFrameRate());
				_ps.setString(31, msg.getNAV1());
				_ps.setString(32, msg.getNAV2());
				_ps.setInt(33, msg.getVASFree());
				_ps.setInt(34, msg.getWeight());
				_ps.setInt(35, msg.getAirspaceType().ordinal());
				_ps.setString(36, msg.getADF1());
				_ps.addBatch();
				
				// Remove entries with no ATC ID
				if (!msg.hasATC())
					i.remove();
			}

			executeBatchUpdate(1, entries.size());
			
			// Write COM/ATC records
			prepareStatementWithoutLimits("REPLACE INTO acars.POSITION_ATC (FLIGHT_ID, REPORT_TIME, IDX, COM1, CALLSIGN, NETWORK_ID, LAT, LNG) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
			for (PositionMessage msg : entries) {
				_ps.setInt(1, msg.getFlightID());
				_ps.setTimestamp(2, createTimestamp(msg.getDate()));
				if (msg.getATC1() != null) {
					_ps.setInt(3, 1);
					_ps.setString(4, msg.getCOM1());
					_ps.setString(5, msg.getATC1().getCallsign());
					_ps.setInt(6, msg.getATC1().getID());
					_ps.setDouble(7, msg.getATC1().getLatitude());
					_ps.setDouble(8, msg.getATC1().getLongitude());
					_ps.addBatch();
				}

				if (msg.getATC2() != null) {
					_ps.setInt(3, 2);
					_ps.setString(4, msg.getCOM2());
					_ps.setString(5, msg.getATC2().getCallsign());
					_ps.setInt(6, msg.getATC2().getID());
					_ps.setDouble(7, msg.getATC2().getLatitude());
					_ps.setDouble(8, msg.getATC2().getLongitude());
					_ps.addBatch();
				}
			}
			
			executeBatchUpdate(1, entries.size());
			commitTransaction();
		} catch (SQLException se) {
			rollbackTransaction();
			throw new DAOException(se);
		}
	}
}