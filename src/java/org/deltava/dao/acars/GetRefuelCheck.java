// Copyright 2018, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;
import java.util.*;
import java.time.Instant;

import org.deltava.beans.acars.*;

import org.deltava.dao.*;

/**
 * A Data Access Object to check for in-flight refueling from non-serialized ACARS position records.
 * @author Luke
 * @version 9.0
 * @since 8.5
 */

public class GetRefuelCheck extends DAO {

	private class FuelData implements FuelChecker, Comparable<FuelChecker> {
		private final Instant _dt; 
		private final int _fuelRemaining;
		private final int _flags;
		
		FuelData(Instant dt, int fuel, int flags) {
			super();
			_dt = dt;
			_fuelRemaining = fuel;
			_flags = flags;
		}
		
		@Override
		public Instant getDate() {
			return _dt;
		}

		@Override
		public int getFuelRemaining() {
			return _fuelRemaining;
		}

		@Override
		public boolean isFlagSet(ACARSFlags flag) {
			return flag.has(_flags);
		}

		@Override
		public int compareTo(FuelChecker fc2) {
			return _dt.compareTo(fc2.getDate());
		}
	}
	
	/**
	 * Initializes the Data Access Object.
	 * @param c the JDBC connection to use
	 */
	public GetRefuelCheck(Connection c) {
		super(c);
	}
	
	/**
	 * Checks if in-flight refueling was used on a Flight.
	 * @param flightID the ACARS Flight ID
	 * @return a List of FuelChecker beans
	 * @throws DAOException if a JDBC error occurs
	 */
	public List<? extends FuelChecker> checkRefuel(int flightID) throws DAOException {
		try (PreparedStatement ps = prepareWithoutLimits("SELECT REPORT_TIME, FUEL, FLAGS FROM acars.POSITIONS WHERE (FLIGHT_ID=?) ORDER BY REPORT_TIME")) {
			ps.setInt(1, flightID);
			try (ResultSet rs = ps.executeQuery()) {
				List<FuelData> data = new ArrayList<FuelData>();
				 while (rs.next()) {
					 FuelData fd = new FuelData(toInstant(rs.getTimestamp(1)), rs.getInt(2), rs.getInt(3));
					 data.add(fd);
				 }
				 
				 return data;
			}
		} catch (SQLException se) {
			throw new DAOException(se);
		}
	}
}