// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2016, 2017, 2018, 2019, 2020, 2021, 2022, 2023 Global Virtual Airlnes Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;

import org.deltava.acars.beans.ACARSConnection;
import org.deltava.acars.message.InfoMessage;

import org.deltava.dao.*;

/**
 * A Data Access Object to write Flight Information entries.
 * @author Luke
 * @version 11.1
 * @since 1.0
 */

public class SetInfo extends DAO {

	private static final String ISQL = "INSERT INTO acars.FLIGHTS (PILOT_ID, AIRLINE, FLIGHT, CREATED, EQTYPE, CRUISE_ALT, AIRPORT_D, AIRPORT_A, AIRPORT_L, ROUTE, "
		+ "REMARKS, FSVERSION, SCHED_VALID, DISPATCHER, MP, REMOTE_ADDR, REMOTE_HOST, CLIENT_BUILD, BETA_BUILD, SIM_MAJOR, SIM_MINOR, TX, APTYPE, "
		+ "PLATFORM, IS64, ACARS64) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, INET6_ATON(?), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final String USQL = "UPDATE acars.FLIGHTS SET PILOT_ID=?, AIRLINE=?, FLIGHT=?, CREATED=?, EQTYPE=?, CRUISE_ALT=?, AIRPORT_D=?, AIRPORT_A=?, "
		+ "AIRPORT_L=?, ROUTE=?, REMARKS=?, FSVERSION=?, SCHED_VALID=?, DISPATCHER=?, MP=?, REMOTE_ADDR=INET6_ATON(?), REMOTE_HOST=?, CLIENT_BUILD=?, "
		+ "BETA_BUILD=?, SIM_MAJOR=?, SIM_MINOR=?, TX=?, APTYPE=?, PLATFORM=?, IS64=?, ACARS64=?, END_TIME=NULL WHERE (ID=?) LIMIT 1";
	
	/**
	 * Initialize the Data Access Object.
	 * @param c the JDBC connection to use
	 */
	public SetInfo(Connection c) {
		super(c);
	}
	
	/**
	 * Writes a Flight Information entry to the database. <i>This call handles INSERTs and UPDATEs</i>
	 * @param ac the ACARSConnection object
	 * @param msg the InfoMessage bean
	 * @throws DAOException if a JDBC error occurs
	 */
	public void write(ACARSConnection ac, InfoMessage msg) throws DAOException {
		boolean isNew = (msg.getFlightID() == 0);
		try {
			startTransaction();
			try (PreparedStatement ps = prepareWithoutLimits(isNew ? ISQL : USQL)) {
				ps.setInt(1, ac.getUser().getID());
				ps.setString(2,  msg.getAirline().getCode());
				ps.setInt(3, msg.getFlightNumber());
				ps.setTimestamp(4, createTimestamp(msg.getStartTime()));
				ps.setString(5, msg.getEquipmentType());
				ps.setString(6, msg.getAltitude());
				ps.setString(7, msg.getAirportD().getIATA());
				ps.setString(8, msg.getAirportA().getIATA());
				ps.setString(9, (msg.getAirportL() == null) ? null : msg.getAirportL().getIATA());
				ps.setString(10, msg.getRoute());
				ps.setString(11, msg.getComments());
				ps.setInt(12, msg.getSimulator().getCode());
				ps.setBoolean(13, msg.isScheduleValidated());
				ps.setInt(14, msg.getDispatcher().ordinal());
				ps.setBoolean(15, false);
				ps.setString(16, ac.getRemoteAddr());
				ps.setString(17, ac.getRemoteHost());
				ps.setInt(18, ac.getClientBuild());
				ps.setInt(19, ac.getBeta());
				ps.setInt(20, msg.getSimMajor());
				ps.setInt(21, msg.getSimMinor());
				ps.setInt(22, msg.getTX());
				ps.setInt(23, msg.getAutopilotType().ordinal());
				ps.setInt(24, msg.getPlatform().ordinal());
				ps.setBoolean(25, msg.getIsSim64Bit());
				ps.setBoolean(26, msg.getIsACARS64Bit());
				if (!isNew)
					ps.setInt(27, msg.getFlightID());
			
				executeUpdate(ps, 1);
			}
			
			// If we're writing a new entry, get the database ID
			int newID = isNew ? getNewID() : msg.getFlightID();
			
			// Write dispatcher ID
			if (isNew && (msg.getDispatcherID() != 0)) {
				try (PreparedStatement ps = prepareWithoutLimits("INSERT INTO acars.FLIGHT_DISPATCHER (ID, DISPATCHER_ID) VALUES (?, ?)")) {
					ps.setInt(1, newID);
					ps.setInt(2, msg.getDispatcherID());
					executeUpdate(ps, 1);
				}
			}
			
			// Save route usage if using auto-Dispatch
			if (isNew && (msg.getRouteID() != 0)) {
				try (PreparedStatement ps = prepareWithoutLimits("INSERT INTO acars.FLIGHT_DISPATCH (ID, ROUTE_ID) VALUES (?, ?)")) {
					ps.setInt(1, newID);
					ps.setInt(2, msg.getRouteID());
					executeUpdate(ps, 0);
				}
				
				// Save route usage
				try (PreparedStatement ps = prepareWithoutLimits("UPDATE acars.ROUTES SET USED=USED+1, LASTUSED=NOW() WHERE (ID=?) LIMIT 1")) {
					ps.setInt(1, msg.getRouteID());
					executeUpdate(ps, 0);
				}
			}
			
			// Write load data
			try (PreparedStatement ps = prepareWithoutLimits("REPLACE INTO acars.FLIGHT_LOAD (ID, PAX, SEATS, LOADTYPE, LOADFACTOR) VALUES (?, ?, ?, ?, ?)")) {
				ps.setInt(1, newID);
				ps.setInt(2, msg.getPassengers());
				ps.setInt(3, msg.getSeats());
				ps.setInt(4, msg.getLoadType().ordinal());
				ps.setDouble(5, msg.getLoadFactor());
				executeUpdate(ps, 1);
			}
				
			commitTransaction();
			msg.setFlightID(newID);
		} catch (SQLException se) {
			rollbackTransaction();
			throw new DAOException(se);
		}
	}
	
	/**
	 * Marks a Flight as complete.
	 * @param flightID the Flight ID
	 * @param force mark the flight closed even if already closed
	 * @throws DAOException if a JDBC error occurs
	 */
	public void close(int flightID, boolean force) throws DAOException {
	   
	   // Build the SQL statement
	   StringBuilder sqlBuf = new StringBuilder("UPDATE acars.FLIGHTS SET END_TIME=NOW() WHERE (ID=?)");
	   if (!force)
	      sqlBuf.append(" AND (END_TIME IS NULL)");
	   
	   try (PreparedStatement ps = prepare(sqlBuf.toString())) {
	      ps.setInt(1, flightID);
	      executeUpdate(ps, 0);
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
	   try (PreparedStatement ps = prepareWithoutLimits("UPDATE acars.FLIGHTS SET PIREP=? WHERE (ID=?) LIMIT 1")) {
	      ps.setBoolean(1, true);
	      ps.setInt(2, flightID);
	      executeUpdate(ps, 0);
	   } catch (SQLException se) {
	      throw new DAOException(se);
	   }
	}
}