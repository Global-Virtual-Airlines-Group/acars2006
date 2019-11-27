// Copyright 2004, 2005, 2006, 2007, 2010, 2012, 2014, 2016, 2017, 2018, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;
import java.util.*;

import org.deltava.dao.*;

import org.deltava.acars.message.PositionMessage;

/**
 * A Data Access Object to write ACARS Position Messages.
 * @author Luke
 * @version 9.0
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
			try (PreparedStatement ps = prepareWithoutLimits("REPLACE INTO acars.POSITIONS (FLIGHT_ID, REPORT_TIME, SIM_TIME, LAT, LNG, B_ALT, R_ALT, HEADING, ASPEED, GSPEED, VSPEED, N1, N2, MACH, "
				+ "FUEL, PHASE, SIM_RATE, FLAGS, GNDFLAGS, FLAPS, PITCH, BANK, FUELFLOW, WIND_HDG, WIND_SPEED, TEMP, PRESSURE, VIZ, AOA, GFORCE, FRAMERATE, NAV1, NAV2, VAS, WEIGHT, ASTYPE, "
				+ "ADF1, NET_CONNECTED) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

				// Write core entries
				startTransaction();
				for (Iterator<PositionMessage> i = entries.iterator(); i.hasNext();) {
					PositionMessage msg = i.next();

					// Set the prepared statement parameters
					ps.setInt(1, msg.getFlightID());
					ps.setTimestamp(2, createTimestamp(msg.getDate()));
					ps.setTimestamp(3, createTimestamp(msg.getSimTime()));
					ps.setDouble(4, msg.getLatitude());
					ps.setDouble(5, msg.getLongitude());
					ps.setInt(6, msg.getAltitude());
					ps.setInt(7, msg.getRadarAltitude());
					ps.setInt(8, msg.getHeading());
					ps.setInt(9, msg.getAspeed());
					ps.setInt(10, msg.getGspeed());
					ps.setInt(11, msg.getVspeed());
					ps.setDouble(12, msg.getN1());
					ps.setDouble(13, msg.getN2());
					ps.setDouble(14, msg.getMach());
					ps.setInt(15, msg.getFuelRemaining());
					ps.setInt(16, msg.getPhase().ordinal());
					ps.setInt(17, msg.getSimRate());
					ps.setInt(18, msg.getFlags());
					ps.setInt(19, msg.getGroundOperations());
					ps.setInt(20, msg.getFlaps());
					ps.setDouble(21, msg.getPitch());
					ps.setDouble(22, msg.getBank());
					ps.setInt(23, msg.getFuelFlow());
					ps.setInt(24, msg.getWindHeading());
					ps.setInt(25, msg.getWindSpeed());
					ps.setInt(26, msg.getTemperature());
					ps.setInt(27, msg.getPressure());
					ps.setDouble(28, msg.getVisibility());
					ps.setDouble(29, msg.getAngleOfAttack());
					ps.setDouble(30, msg.getG());
					ps.setInt(31, msg.getFrameRate());
					ps.setString(32, msg.getNAV1());
					ps.setString(33, msg.getNAV2());
					ps.setInt(34, msg.getVASFree());
					ps.setInt(35, msg.getWeight());
					ps.setInt(36, msg.getAirspaceType().ordinal());
					ps.setString(37, msg.getADF1());
					ps.setBoolean(38, msg.getNetworkConnected());
					ps.addBatch();

					// Remove entries with no ATC ID
					if (!msg.hasATC()) i.remove();
				}

				executeUpdate(ps, 1, entries.size());
			}

			// Write COM/ATC records
			try (PreparedStatement ps = prepareWithoutLimits("REPLACE INTO acars.POSITION_ATC (FLIGHT_ID, REPORT_TIME, IDX, COM1, CALLSIGN, NETWORK_ID, LAT, LNG) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
				for (PositionMessage msg : entries) {
					ps.setInt(1, msg.getFlightID());
					ps.setTimestamp(2, createTimestamp(msg.getDate()));
					if (msg.getATC1() != null) {
						ps.setInt(3, 1);
						ps.setString(4, msg.getCOM1());
						ps.setString(5, msg.getATC1().getCallsign());
						ps.setInt(6, msg.getATC1().getID());
						ps.setDouble(7, msg.getATC1().getLatitude());
						ps.setDouble(8, msg.getATC1().getLongitude());
						ps.addBatch();
					}

					if (msg.getATC2() != null) {
						ps.setInt(3, 2);
						ps.setString(4, msg.getCOM2());
						ps.setString(5, msg.getATC2().getCallsign());
						ps.setInt(6, msg.getATC2().getID());
						ps.setDouble(7, msg.getATC2().getLatitude());
						ps.setDouble(8, msg.getATC2().getLongitude());
						ps.addBatch();
					}
				}

				executeUpdate(ps, 1, entries.size());
			}

			commitTransaction();
		} catch (SQLException se) {
			rollbackTransaction();
			throw new DAOException(se);
		}
	}
}