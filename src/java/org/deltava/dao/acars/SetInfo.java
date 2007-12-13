// Copyright 2004, 2005, 2006, 2007 Global Virtual Airlnes Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;
import java.util.*;

import org.deltava.acars.message.InfoMessage;

import org.deltava.beans.navdata.*;
import org.deltava.dao.*;

/**
 * A Data Access Object to write Flight Information entries.
 * @author Luke
 * @version 2.1
 * @since 1.0
 */

public final class SetInfo extends DAO {

	// SQL update statements
	private static final String ISQL = "INSERT INTO acars.FLIGHTS (CON_ID, FLIGHT_NUM, CREATED, EQTYPE, CRUISE_ALT, "
		+ "AIRPORT_D, AIRPORT_A, AIRPORT_L, ROUTE, REMARKS, FSVERSION, OFFLINE, SCHED_VALID, DISPATCH_PLAN) "
		+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final String USQL = "UPDATE acars.FLIGHTS SET CON_ID=?, FLIGHT_NUM=?, CREATED=?, EQTYPE=?, "
		+ "CRUISE_ALT=?, AIRPORT_D=?, AIRPORT_A=?, AIRPORT_L=?, ROUTE=?, REMARKS=?, FSVERSION=?, OFFLINE=?, "
		+ "SCHED_VALID=?, DISPATCH_PLAN=?, END_TIME=NULL WHERE (ID=?)";
	
	/**
	 * Initialize the Data Access Object.
	 * @param c the JDBC connection to use
	 */
	public SetInfo(Connection c) {
		super(c);
	}
	
	/**
	 * Writes a Flight Information entry to the database. <i>This call handles INSERTs and UPDATEs</i>
	 * @param msg the InfoMessage bean
	 * @param cid the connection ID
	 * @throws DAOException if a JDBC error occurs
	 */
	public void write(InfoMessage msg, long cid) throws DAOException {
		boolean isNew = (msg.getFlightID() == 0);
		try {
			// Set the prepared statement
			prepareStatement(isNew ? ISQL : USQL);
			_ps.setLong(1, cid);
			_ps.setString(2, msg.getFlightCode());
			_ps.setTimestamp(3, createTimestamp(msg.getStartTime()));
			_ps.setString(4, msg.getEquipmentType());
			_ps.setString(5, msg.getAltitude());
			_ps.setString(6, msg.getAirportD().getIATA());
			_ps.setString(7, msg.getAirportA().getIATA());
			_ps.setString(8, (msg.getAirportL() == null) ? null : msg.getAirportL().getIATA());
			_ps.setString(9, msg.getAllWaypoints());
			_ps.setString(10, msg.getComments());
			_ps.setInt(11, msg.getFSVersion());
			_ps.setBoolean(12, msg.isOffline());
			_ps.setBoolean(13, msg.isScheduleValidated());
			_ps.setBoolean(14, msg.isDispatchPlan());
			if (msg.getFlightID() != 0)
				_ps.setInt(15, msg.getFlightID());
			
			// Write to the database
			executeUpdate(0);
			
			// If we're writing a new entry, get the database ID otherwise clean out SID/STAR data
			if (isNew)
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
	   StringBuilder sqlBuf = new StringBuilder("UPDATE acars.FLIGHTS SET END_TIME=NOW() WHERE (ID=?) AND (CON_ID=?)");
	   if (!force)
	      sqlBuf.append(" AND (END_TIME IS NULL)");
	   
	   try {
	      prepareStatement(sqlBuf.toString());
	      _ps.setInt(1, flightID);
	      _ps.setLong(2, cid);
	      executeUpdate(0);
	   } catch (SQLException se) {
	      throw new DAOException(se);
	   }
	}
	
	/**
	 * Deletes a Flight's SID/STAR data from the datbase.
	 * @param id the Flight ID
	 * @throws DAOException if a JDBC error occurs
	 */
	public void clearSIDSTAR(int id) throws DAOException {
		try {
			prepareStatementWithoutLimits("DELETE FROM acars.FLIGHT_SIDSTAR WHERE (ID=?)");
			_ps.setInt(1, id);
			executeUpdate(0);
		} catch (SQLException se) {
			throw new DAOException(se);
		}
	}
	
	/**
	 * Writes a Flight's SID/STAR data to the database.
	 * @param id the Flight ID
	 * @param tr the TerminalRoute bean
	 * @throws DAOException if a JDBC error occurs
	 */
	public void writeSIDSTAR(int id, TerminalRoute tr) throws DAOException {
		if (tr == null)
			return;
		
		try {
			startTransaction();
			
			// Write the Route
			if (tr != null) {
				prepareStatementWithoutLimits("REPLACE INTO acars.FLIGHT_SIDSTAR (ID, TYPE, NAME, TRANSITION, "
						+ "RUNWAY) VALUES (?, ?, ?, ?, ?)");
				_ps.setInt(1, id);
				_ps.setInt(2, tr.getType());
				_ps.setString(3, tr.getName());
				_ps.setString(4, tr.getTransition());
				_ps.setString(5, tr.getRunway());
				executeUpdate(1);
			
				// Write the route data
				prepareStatementWithoutLimits("REPLACE INTO acars.FLIGHT_SIDSTAR_WP (ID, TYPE, SEQ, CODE, LATITUDE, "
						+ "LONGITUDE) VALUES (?, ? ,?, ?, ?, ?)");
				_ps.setInt(1, id);
				_ps.setInt(2, tr.getType());
				for (Iterator<NavigationDataBean> i = tr.getWaypoints().iterator(); i.hasNext(); ) {
					Airway.AirwayIntersection ai = (Airway.AirwayIntersection) i.next();
					_ps.setInt(3, ai.getSequence());
					_ps.setString(4, ai.getCode());
					_ps.setDouble(5, ai.getLatitude());
					_ps.setDouble(6, ai.getLongitude());
					_ps.addBatch();
				}
				
				// Write and clean up
				_ps.executeBatch();
				_ps.close();
			}
			
			// Commit
			commitTransaction();
		} catch (SQLException se) {
			rollbackTransaction();
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
	      prepareStatementWithoutLimits("UPDATE acars.FLIGHTS SET PIREP=? WHERE (ID=?) LIMIT 1");
	      _ps.setBoolean(1, true);
	      _ps.setInt(2, flightID);
	      executeUpdate(0);
	   } catch (SQLException se) {
	      throw new DAOException(se);
	   }
	}
}