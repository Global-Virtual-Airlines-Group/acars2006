// Copyright 2004, 2005, 2006, 2007, 2010, 2012, 2014, 2016, 2017, 2018, 2019, 2020, 2021, 2025 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;
import java.util.*;

import org.deltava.dao.*;

import org.deltava.acars.message.PositionMessage;
import org.deltava.beans.acars.EngineSpeedEncoder;

/**
 * A Data Access Object to write ACARS Position Messages.
 * @author Luke
 * @version 11.5
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
			try (PreparedStatement ps = prepareWithoutLimits("REPLACE INTO acars.POSITIONS (FLIGHT_ID, REPORT_TIME, SIM_TIME, LAT, LNG, B_ALT, R_ALT, ALTIMETER, HEADING, ASPEED, GSPEED, VSPEED, N1, N2, MACH, "
				+ "FUEL, PHASE, SIM_RATE, FLAGS, GNDFLAGS, FLAPS, PITCH, BANK, FUELFLOW, WIND_HDG, WIND_SPEED, TEMP, PRESSURE, VIZ, AOA, CG, GFORCE, FRAMERATE, NAV1, NAV2, VAS, WEIGHT, ASTYPE, ADF1, NET_CONNECTED, "
				+ "ACARS_CONNECTED, RESTORE_COUNT, ENC_N1, ENC_N2) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {

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
					ps.setInt(8, msg.getAltimeter());
					ps.setInt(9, msg.getHeading());
					ps.setInt(10, msg.getAspeed());
					ps.setInt(11, msg.getGspeed());
					ps.setInt(12, msg.getVspeed());
					ps.setDouble(13, msg.getAverageN1());
					ps.setDouble(14, msg.getAverageN2());
					ps.setDouble(15, msg.getMach());
					ps.setInt(16, msg.getFuelRemaining());
					ps.setInt(17, msg.getPhase().ordinal());
					ps.setInt(18, msg.getSimRate());
					ps.setInt(19, msg.getFlags());
					ps.setInt(20, msg.getGroundOperations());
					ps.setInt(21, msg.getFlaps());
					ps.setDouble(22, msg.getPitch());
					ps.setDouble(23, msg.getBank());
					ps.setInt(24, msg.getFuelFlow());
					ps.setInt(25, msg.getWindHeading());
					ps.setInt(26, msg.getWindSpeed());
					ps.setInt(27, msg.getTemperature());
					ps.setInt(28, msg.getPressure());
					ps.setDouble(29, msg.getVisibility());
					ps.setDouble(30, msg.getAngleOfAttack());
					ps.setDouble(31, msg.getCG());
					ps.setDouble(32, msg.getG());
					ps.setInt(33, msg.getFrameRate());
					ps.setString(34, msg.getNAV1());
					ps.setString(35, msg.getNAV2());
					ps.setInt(36, msg.getVASFree());
					ps.setInt(37, msg.getWeight());
					ps.setInt(38, msg.getAirspaceType().ordinal());
					ps.setString(39, msg.getADF1());
					ps.setBoolean(40, msg.getNetworkConnected());
					ps.setBoolean(41, msg.getACARSConnected());
					ps.setInt(42, msg.getRestoreCount());
					ps.setBytes(43, EngineSpeedEncoder.encode(msg.getEngineCount(), msg.getN1()));
					ps.setBytes(44, EngineSpeedEncoder.encode(msg.getEngineCount(), msg.getN2()));
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