// Copyright 2016, 2019, 2021, 2022 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;

import org.deltava.acars.message.*;

import org.deltava.beans.Simulator;
import org.deltava.beans.acars.*;

import org.deltava.dao.*;

import org.deltava.util.StringUtils;

/**
 * A Data Access Object to write user system data to the database.
 * @author Luke
 * @version 10.2
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
			startTransaction();
			
			// Write core data
			try (PreparedStatement ps = prepareWithoutLimits("REPLACE INTO acars.SYSINFO (ID, CREATED, OS_VERSION, CLR_VERSION, NET_VERSION, IS64, SLI, LOCALE, TZ, MEMORY, CPU, CPU_SPEED, CPU_SOCK, "
				+ "CPU_CORE, CPU_PROC, GPU, GPU_DRIVER, VRAM, X, Y, BPP, SCREENS) VALUES (?, NOW(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
				ps.setInt(1,  msg.getSender().getID());
				ps.setString(2, msg.getOSVersion());
				ps.setString(3, msg.getCLRVersion());
				ps.setString(4, msg.getDotNETVersion());
				ps.setBoolean(5, msg.is64Bit());
				ps.setBoolean(6, msg.isSLI());
				ps.setString(7, msg.getLocale());
				ps.setString(8, msg.getTimeZone());
				ps.setInt(9, msg.getMemorySize());
				ps.setString(10, msg.getCPU());
				ps.setInt(11, msg.getCPUSpeed());
				ps.setInt(12, msg.getSockets());
				ps.setInt(13, msg.getCores());
				ps.setInt(14, msg.getThreads());
				ps.setString(15, msg.getGPU());
				ps.setString(16, msg.getGPUDriverVersion());
				ps.setInt(17, msg.getVideoMemorySize());
				ps.setInt(18, msg.getWidth());
				ps.setInt(19, msg.getHeight());
				ps.setInt(20, msg.getColorDepth());
				ps.setInt(21, msg.getScreenCount());
				executeUpdate(ps, 1);
			}
			
			// Write sim data
			if ((msg.getSimulator() != Simulator.UNKNOWN) && (!StringUtils.isEmpty(msg.getBridgeInfo()))) {
				try (PreparedStatement ps = prepareWithoutLimits("INSERT INTO acars.SIMINFO (ID, CREATED, SIM, BRIDGE) VALUES (?, NOW(), ?, ?)")) {
					ps.setInt(1,  msg.getSender().getID());	
					ps.setInt(2, msg.getSimulator().ordinal());
					ps.setString(3, msg.getBridgeInfo());
					executeUpdate(ps, 1);
				}
			}
			
			commitTransaction();
		} catch (SQLException se) {
			rollbackTransaction();
			throw new DAOException(se);
		}
	}
	
	/**
	 * Writes a PerformanceMessage to the database.
	 * @param pm a PerformanceMessage
	 * @throws DAOException if a JDBC error occurs
	 */
	public void write(PerformanceMessage pm) throws DAOException {
		try {
			startTransaction();
			
			// Clean up, since this is flight ID related
			try (PreparedStatement ps = prepareWithoutLimits("DELETE FROM acars.PERFINFO WHERE (ID=?)")) {
				ps.setInt(1, pm.getFlightID());
				executeUpdate(ps, 0);
			}
			
			try (PreparedStatement ps = prepareWithoutLimits("DELETE FROM acars.PERFCOUNTER WHERE (ID=?)")) {
				ps.setInt(1, pm.getFlightID());
				executeUpdate(ps, 0);
			}
			
			// Write the timers
			if (pm.hasTimers()) {
				try (PreparedStatement ps = prepareWithoutLimits("INSERT INTO acars.PERFINFO (ID, TIMER, TICKSIZE, COUNT, TOTAL, MAX, MIN, STDDEV) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
					ps.setInt(1, pm.getFlightID());
					for (TaskTimerData ttd : pm.getTimers()) {
						ps.setString(2, ttd.getName());
						ps.setInt(3, ttd.getTickSize());
						ps.setLong(4, ttd.getCount());
						ps.setLong(5, ttd.getTotal());
						ps.setInt(6, ttd.getMax());
						ps.setInt(7, ttd.getMin());
						ps.setDouble(8, ttd.getStdDev());
						ps.addBatch();
					}
			
					executeUpdate(ps, 1, pm.getTimers().size());
				}
			}
			
			// Write the counters
			if (pm.hasCounters()) {
				try (PreparedStatement ps = prepareWithoutLimits("INSERT INTO acars.PERFCOUNTER (ID, NAME, VALUE) VALUES (?, ?, ?)")) {
					ps.setInt(1, pm.getFlightID());
					for (String ctrName : pm.getCounters()) {
						ps.setString(2, ctrName);
						ps.setInt(3, pm.getCounter(ctrName, 0));
						ps.addBatch();
					}
				
					executeUpdate(ps, 1, pm.getCounters().size());
				}
			}
			
			commitTransaction();
		} catch (SQLException se) {
			rollbackTransaction();
			throw new DAOException(se);
		}
	}

	/**
	 * Writes frame rate data to the database.
	 * @param fr a FrameRates bean
	 * @throws DAOException if a JDBC error occurs
	 */
	public void write(FrameRates fr) throws DAOException {
		try (PreparedStatement ps = prepareWithoutLimits("REPLACE INTO acars.FRAMERATES (ID, SIZE, MIN, MAX, P1, P5, P50, P95, P99, AVERAGE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
			ps.setInt(1, fr.getID());
			ps.setInt(2, fr.getSize());
			ps.setInt(3, fr.getMin());
			ps.setInt(4, fr.getMax());
			ps.setInt(5, fr.getPercentile(1));
			ps.setInt(6, fr.getPercentile(5));
			ps.setInt(7, fr.getPercentile(50));
			ps.setInt(8, fr.getPercentile(95));
			ps.setInt(9, fr.getPercentile(99));
			ps.setDouble(10, fr.getAverage());
			executeUpdate(ps, 1);
		} catch (SQLException se) {
			throw new DAOException(se);
		}
	}
}