// Copyright 2004, 2005, 2006, 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

import org.deltava.dao.*;

import org.deltava.acars.message.PositionMessage;

import org.deltava.util.CalendarUtils;

/**
 * A Data Access Object to write ACARS Position Messages. This DAO is unique in that it maintains an internal queue of messages,
 * and only flushes this queue upon request. This behavior is designed to avoid making large number of connection pool requests,
 * since ACARS positions may be written several times a second by the server.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class SetPosition extends DAO {
	
	private static final BlockingQueue<PositionCacheEntry> _queue = new LinkedBlockingQueue<PositionCacheEntry>();
	private static long _maxAge = -1; 

	private static class PositionCacheEntry {
		
		private PositionMessage _msg;
		private long _conID;
		private int _flightID;
		
		PositionCacheEntry(PositionMessage msg, long conID, int flightID) {
			super();
			_msg = msg;
			_conID = conID;
			_flightID = flightID;
		}
		
		public PositionMessage getMessage() {
			return _msg;
		}
		
		public long getConnectionID() {
			return _conID;
		}
		
		public int getFlightID() {
			return _flightID;
		}
	}
	
	/**
	 * Initializes the Data Access Object.
	 * @param c the JDBC connection to use
	 */
	public SetPosition(Connection c) {
		super(c);
	}
	
	/**
	 * Adds a Position report to the queue.
	 * @param msg the PositionMessage bean
	 * @param cid the connection ID
	 * @param flightID the flight ID
	 * @see SetPosition#flush()
	 */
	public static void queue(PositionMessage msg, long cid, int flightID) {
		if (_queue.isEmpty())
			_maxAge = System.currentTimeMillis();
		
		_queue.add(new PositionCacheEntry(msg, cid, flightID));
	}
	
	/**
	 * Returns the number of entries in the queue.
	 * @return the size of the queue
	 */
	public static int size() {
		return _queue.size();
	}
	
	/**
	 * Returns the age of the oldest entry in the queue.
	 * @return the age in millseconds
	 */
	public static long getMaxAge() {
		return (_queue.isEmpty()) ? 0 : (System.currentTimeMillis() - _maxAge);
	}
	
	/**
	 * Flushes the queue to the database.
	 * @return the number of entries written
	 * @throws DAOException if a JDBC error occurs
	 */
	public int flush() throws DAOException {
		try {
			prepareStatementWithoutLimits("INSERT INTO acars.POSITIONS (CON_ID, FLIGHT_ID, REPORT_TIME, TIME_MS, LAT, LNG, B_ALT, "
					+ "R_ALT, HEADING, ASPEED, GSPEED, VSPEED, N1, N2, MACH, FUEL, PHASE, SIM_RATE, FLAGS, FLAPS, PITCH, BANK, "
					+ "FUELFLOW, WIND_HDG, WIND_SPEED, AOA, GFORCE, FRAMERATE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
					+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			
			// Drain the queue
			Collection<PositionCacheEntry> entries = new ArrayList<PositionCacheEntry>();
			int results = _queue.drainTo(entries);
			for (Iterator<PositionCacheEntry> i = entries.iterator(); i.hasNext(); ) {
				PositionCacheEntry entry = i.next();
				PositionMessage msg = entry.getMessage();
				
				// Build the timestamp
				Calendar cld = CalendarUtils.getInstance(msg.getDate());
				int ms = cld.get(Calendar.MILLISECOND);

				// Set the prepared statement parameters
				_ps.setLong(1, entry.getConnectionID());
				_ps.setInt(2, entry.getFlightID());
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
				_ps.addBatch();
			}

			// Do the update
			_ps.executeBatch();
			_ps.close();
			return results;
		} catch (SQLException se) {
			throw new DAOException(se);
		}
	}
}