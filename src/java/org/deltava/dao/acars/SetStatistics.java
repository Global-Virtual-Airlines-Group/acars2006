// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

import org.deltava.acars.beans.CommandEntry;

import org.deltava.dao.*;
import org.deltava.util.CalendarUtils;

/**
 * A Data Access Object to log ACARS command statistics.
 * @author Luke
 * @version 1.0
 * @since 1.1
 */

public class SetStatistics extends DAO implements FlushableDAO<CommandEntry> {
	
	private static final BlockingQueue<CommandEntry> _queue = new LinkedBlockingQueue<CommandEntry>();
	private static long _maxAge = -1;

	/**
	 * Initializes the Data Access Object.
	 * @param c the JDBC Connection to use
	 */
	public SetStatistics(Connection c) {
		super(c);
	}
	
	/**
	 * Adds an ACARS command log entry to the buffer.
	 * @param ce the log entry
	 */
	public static void queue(CommandEntry ce) {
		if (_queue.isEmpty())
			_maxAge = System.currentTimeMillis();

		_queue.add(ce);
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
	 * Flushes command execution log entries to the database.
	 * @return the number of rows written
	 * @throws DAOException if a JDBC error occurs
	 */
	public int flush() throws DAOException {
		Collection<CommandEntry> entries = new ArrayList<CommandEntry>();
		int rows = _queue.drainTo(entries);
		try {
			prepareStatement("INSERT INTO acars.COMMAND_STATS (CMDDATE, MS, ID, CLASS, EXECTIME) VALUES (?, ?, ?, ?, ?) "
					+ "ON DUPLICATE KEY UPDATE EXECTIME=?");
			for (Iterator<CommandEntry> i = entries.iterator(); i.hasNext(); ) {
				CommandEntry e = i.next();
				
				// Build the timestamp
				Calendar cld = CalendarUtils.getInstance(e.getDate());
				
				// Update the prepared statement
				_ps.setTimestamp(1, createTimestamp(e.getDate()));
				_ps.setInt(2, cld.get(Calendar.MILLISECOND));
				_ps.setInt(3, e.getID());
				_ps.setString(4, e.getName());
				_ps.setLong(5, e.getExecTime());
				_ps.setLong(6, e.getExecTime());
				_ps.addBatch();
			}
			
			// Execute and clean up
			_ps.executeBatch();
			_ps.close();
		} catch (SQLException se) {
			throw new DAOException(se);
		}
		
		return rows;
	}
}