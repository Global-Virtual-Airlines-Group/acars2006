// Copyright 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;

import org.deltava.acars.message.SystemInfoMessage;

import org.deltava.dao.*;

/**
 * A Data Access Object to write user system data to the database.
 * @author Luke
 * @version 6.4
 * @since 6.4
 */

public class SetSystemInfo extends DAO {

	/**
	 * Initializes the Data Access Object.
	 * @param c the JDBC connection to use
	 */
	public SetSystemInfo(Connection c) {
		super(c);
	}

	/**
	 * Writes a System Information message to the database.
	 * @param msg the Message
	 * @throws DAOException if a JDBC error occurs
	 */
	public void write(SystemInfoMessage msg) throws DAOException {
		try {
			prepareStatementWithoutLimits("REPLACE INTO acars.SYSINFO (ID, CREATED, OS_VERSION, CLR_VERSION, NET_VERSION, IS64, SLI, "
				+ "LOCALE, TZ, MEMORY, CPU, CPU_SPEED, CPU_SOCK, CPU_CORE, CPU_PROC, GPU, GPU_DRIVER, VRAM, X, Y, BPP, SCREENS) "
				+ "VALUES (?, NOW(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			_ps.setInt(1,  msg.getSender().getID());
			_ps.setString(2, msg.getOSVersion());
			_ps.setString(3, msg.getCLRVersion());
			_ps.setString(4, msg.getDotNETVersion());
			_ps.setBoolean(5, msg.is64Bit());
			_ps.setBoolean(6, msg.isSLI());
			_ps.setString(7, msg.getLocale());
			_ps.setString(8, msg.getTimeZone());
			_ps.setInt(9, msg.getMemorySize());
			_ps.setString(10, msg.getCPU());
			_ps.setInt(11, msg.getCPUSpeed());
			_ps.setInt(12, msg.getSockets());
			_ps.setInt(13, msg.getCores());
			_ps.setInt(14, msg.getThreads());
			_ps.setString(15, msg.getGPU());
			_ps.setString(16, msg.getGPUDriverVersion());
			_ps.setInt(17, msg.getVideoMemorySize());
			_ps.setInt(18, msg.getWidth());
			_ps.setInt(19, msg.getHeight());
			_ps.setInt(20, msg.getColorDepth());
			_ps.setInt(21, msg.getScreenCount());
			executeUpdate(1);
		} catch (SQLException se) {
			throw new DAOException(se);
		}
	}
}