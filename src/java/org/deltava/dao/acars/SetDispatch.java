// Copyright 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;

import org.deltava.beans.Pilot;
import org.deltava.dao.*;

import org.deltava.acars.message.dispatch.FlightDataMessage;

/**
 * A Data Access Object to write ACARS Dispatch log entries.
 * @author Luke
 * @version 9.0
 * @since 9.0
 */

public class SetDispatch extends DAO {

	/**
	 * Initializes the Data Access Object.
	 * @param c the JDBC connection to use
	 */
	public SetDispatch(Connection c) {
		super(c);
	}

	/**
	 * Logs an ACARS Dispatch flight data message.
	 * @param msg a FlightDataMessage
	 * @param p the receiving Pilot
	 * @throws DAOException if a JDBC error occurs
	 */
	public void writeLog(FlightDataMessage msg, Pilot p) throws DAOException {
		
		int totalFuel = msg.getFuel().values().stream().mapToInt(Integer::intValue).sum();
		try {
			startTransaction();
			try (PreparedStatement ps = prepareWithoutLimits("INSERT INTO acars.DISPATCH_LOG (CREATED, DISPATCHER_ID, PILOT_ID, EQTYPE, CRUISE_ALT, AIRPORT_D, AIRPORT_A, FSVERSION, FUEL, "
				+ "SID, STAR, ROUTE) VALUES (NOW(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
				ps.setInt(1, msg.getSender().getID());
				ps.setInt(2, p.getID());
				ps.setString(3, msg.getEquipmentType());
				ps.setString(4, msg.getCruiseAltitude());
				ps.setString(5, msg.getAirportD().getIATA());
				ps.setString(6, msg.getAirportA().getIATA());
				ps.setInt(7, msg.getSimulator().getCode());
				ps.setInt(8, totalFuel);
				ps.setString(9, msg.getSID());
				ps.setString(10, msg.getSTAR());
				ps.setString(11, msg.getRoute());
				executeUpdate(ps, 1);
			}
			
			msg.setLogID(getNewID());
			commitTransaction();
		} catch (SQLException se) {
			rollbackTransaction();
			throw new DAOException(se);
		}
	}

	/**
	 * Links an ACARS flight record to an ACARS Dispatch log entry.
	 * @param logID the log entry database ID
	 * @param flightID the ACARS Flight database ID
	 * @throws DAOException if a JDBC error occurs
	 */
	public void link(int logID, int flightID) throws DAOException {
		try (PreparedStatement ps = prepareWithoutLimits("REPLACE INTO acars.FLIGHT_DISPATCH_LOG (ID, LOG_ID) VALUES (?, ?)")) {
			ps.setInt(1, flightID);
			ps.setInt(2, logID);
			executeUpdate(ps, 1);
		} catch (SQLException se) {
			throw new DAOException(se);
		}
	}
}