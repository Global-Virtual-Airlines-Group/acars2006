// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2016, 2017, 2018, 2019 Global Virtual Airlnes Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;

import org.deltava.acars.beans.ACARSConnection;
import org.deltava.acars.message.InfoMessage;

import org.deltava.dao.*;

/**
 * A Data Access Object to write Flight Information entries.
 * @author Luke
 * @version 9.0
 * @since 1.0
 */

public class SetInfo extends DAO {

	private static final String ISQL = "INSERT INTO acars.FLIGHTS (PILOT_ID, FLIGHT_NUM, CREATED, EQTYPE, CRUISE_ALT, AIRPORT_D, AIRPORT_A, AIRPORT_L, ROUTE, "
		+ "REMARKS, FSVERSION, SCHED_VALID, DISPATCH_PLAN, MP, REMOTE_ADDR, REMOTE_HOST, CLIENT_BUILD, BETA_BUILD, SIM_MAJOR, SIM_MINOR, TX, LOADFACTOR, "
		+ "APTYPE, PLATFORM, IS64, ACARS64, LOADTYPE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, INET6_ATON(?), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final String USQL = "UPDATE acars.FLIGHTS SET PILOT_ID=?, FLIGHT_NUM=?, CREATED=?, EQTYPE=?, CRUISE_ALT=?, AIRPORT_D=?, AIRPORT_A=?, "
		+ "AIRPORT_L=?, ROUTE=?, REMARKS=?, FSVERSION=?, SCHED_VALID=?, DISPATCH_PLAN=?, MP=?, REMOTE_ADDR=INET6_ATON(?), REMOTE_HOST=?, CLIENT_BUILD=?, "
		+ "BETA_BUILD=?, SIM_MAJOR=?, SIM_MINOR=?, TX=?, LOADFACTOR=?, APTYPE=?, PLATFORM=?, IS64=?, ACARS64=?, LOADTYPE=?, END_TIME=NULL WHERE (ID=?) LIMIT 1";
	
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
				ps.setString(2, msg.getFlightCode());
				ps.setTimestamp(3, createTimestamp(msg.getStartTime()));
				ps.setString(4, msg.getEquipmentType());
				ps.setString(5, msg.getAltitude());
				ps.setString(6, msg.getAirportD().getIATA());
				ps.setString(7, msg.getAirportA().getIATA());
				ps.setString(8, (msg.getAirportL() == null) ? null : msg.getAirportL().getIATA());
				ps.setString(9, msg.getRoute());
				ps.setString(10, msg.getComments());
				ps.setInt(11, msg.getSimulator().getCode());
				ps.setBoolean(12, msg.isScheduleValidated());
				ps.setBoolean(13, msg.isDispatchPlan());
				ps.setBoolean(14, (msg.getLivery() != null));
				ps.setString(15, ac.getRemoteAddr());
				ps.setString(16, ac.getRemoteHost());
				ps.setInt(17, ac.getClientBuild());
				ps.setInt(18, ac.getBeta());
				ps.setInt(19, msg.getSimMajor());
				ps.setInt(20, msg.getSimMinor());
				ps.setInt(21, msg.getTX());
				ps.setDouble(22, msg.getLoadFactor());
				ps.setInt(23, msg.getAutopilotType().ordinal());
				ps.setInt(24, msg.getPlatform().ordinal());
				ps.setBoolean(25, msg.getIsSim64Bit());
				ps.setBoolean(26, msg.getIsACARS64Bit());
				ps.setInt(27, msg.getLoadType().ordinal());
				if (!isNew)
					ps.setInt(28, msg.getFlightID());
			
				executeUpdate(ps, 1);
			}
			
			// If we're writing a new entry, get the database ID
			int newID = isNew ? getNewID() : msg.getFlightID();
			
			// Write dispatcher ID
			if (isNew && (msg.getDispatcherID() != 0)) {
				try (PreparedStatement ps = prepareWithoutLimits("INSERT INTO acars.FLIGHT_DISPATCHER (ID, DISPATCHER_ID) VALUES (?, ?)")) {
					ps.setInt(1, newID);
					ps.setInt(2, msg.getDispatcherID());
					executeUpdate(ps, 0);
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