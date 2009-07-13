// Copyright 2004, 2005, 2006, 2007, 2008, 2009 Global Virtual Airlnes Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;

import org.deltava.acars.message.InfoMessage;

import org.deltava.dao.*;

/**
 * A Data Access Object to write Flight Information entries.
 * @author Luke
 * @version 2.6
 * @since 1.0
 */

public class SetInfo extends DAO {

	// SQL update statements
	private static final String ISQL = "INSERT INTO acars.FLIGHTS (CON_ID, FLIGHT_NUM, CREATED, EQTYPE, CRUISE_ALT, "
		+ "AIRPORT_D, AIRPORT_A, AIRPORT_L, ROUTE, REMARKS, FSVERSION, OFFLINE, SCHED_VALID, DISPATCH_PLAN, "
		+ "MP) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final String USQL = "UPDATE acars.FLIGHTS SET CON_ID=?, FLIGHT_NUM=?, CREATED=?, EQTYPE=?, "
		+ "CRUISE_ALT=?, AIRPORT_D=?, AIRPORT_A=?, AIRPORT_L=?, ROUTE=?, REMARKS=?, FSVERSION=?, OFFLINE=?, "
		+ "SCHED_VALID=?, DISPATCH_PLAN=?, MP=?, END_TIME=NULL WHERE (ID=?) LIMIT 1";
	
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
			startTransaction();
			
			prepareStatementWithoutLimits(isNew ? ISQL : USQL);
			_ps.setLong(1, cid);
			_ps.setString(2, msg.getFlightCode());
			_ps.setTimestamp(3, createTimestamp(msg.getStartTime()));
			_ps.setString(4, msg.getEquipmentType());
			_ps.setString(5, msg.getAltitude());
			_ps.setString(6, msg.getAirportD().getIATA());
			_ps.setString(7, msg.getAirportA().getIATA());
			_ps.setString(8, (msg.getAirportL() == null) ? null : msg.getAirportL().getIATA());
			_ps.setString(9, msg.getRoute());
			_ps.setString(10, msg.getComments());
			_ps.setInt(11, msg.getFSVersion());
			_ps.setBoolean(12, msg.isOffline());
			_ps.setBoolean(13, msg.isScheduleValidated());
			_ps.setBoolean(14, msg.isDispatchPlan());
			_ps.setBoolean(15, (msg.getLivery() != null));
			if (msg.getFlightID() != 0)
				_ps.setInt(16, msg.getFlightID());
			
			// Write to the database
			executeUpdate(1);
			
			// If we're writing a new entry, get the database ID
			if (isNew)
				msg.setFlightID(getNewID());
			
			// Save route usage if using auto-Dispatch
			if (isNew && (msg.getRouteID() != 0)) {
				prepareStatementWithoutLimits("INSERT INTO acars.FLIGHT_DISPATCH (ID, ROUTE_ID, DISPATCHER_ID) VALUES (?, ?, ?)");
				_ps.setInt(1, msg.getFlightID());
				_ps.setInt(2, msg.getRouteID());
				_ps.setInt(3, msg.getDispatcherID());
				executeUpdate(0);
				
				// Save route usage
				prepareStatementWithoutLimits("UPDATE acars.ROUTES SET USED=USED+1, LASTUSED=NOW() WHERE (ID=?) LIMIT 1");
				_ps.setInt(1, msg.getRouteID());
				executeUpdate(0);
			}
				
			commitTransaction();
		} catch (SQLException se) {
			rollbackTransaction();
			throw new DAOException(se);
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