// Copyright 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;
import java.util.*;

import org.deltava.acars.beans.PositionCount;

import org.deltava.dao.*;

/**
 * A Data Access Object to load ACARS unserialized position counts. This is primarily used to find duplicate Flight records.
 * @author Luke
 * @version 9.0
 * @since 8.6
 */

public class GetPositionCount extends DAO {

	/**
	 * Initializes the Data Access Object.
	 * @param c the JDBC connection to use
	 */
	public GetPositionCount(Connection c) {
		super(c);
	}

	/**
	 * Returns the number of unserialized positions associated with an ACARS flight.
	 * @param flightID the ACARS flight ID
	 * @return a PositionCount bean
	 * @throws DAOException if a JDBC error occurs
	 */
	public PositionCount getCount(int flightID) throws DAOException {
		try (PreparedStatement ps = prepareWithoutLimits("SELECT COUNT(FLIGHT_ID) FROM acars.POSITIONS WHERE (FLIGHT_ID=?)")) {
			ps.setInt(1, flightID);
			try (ResultSet rs = ps.executeQuery()) {
				return new PositionCount(flightID, rs.next() ? rs.getInt(1) : 0);
			}
		} catch (SQLException se) {
			throw new DAOException(se);
		}
	}
	
	/**
	 * Searches for a duplicate flight ID created at the same time by the same user.
	 * @param flightID the ACARS flight ID
	 * @return the duplicate flight ID, or zero if none found
	 * @throws DAOException if a JDBC error occurs
	 */
	public List<PositionCount> getDuplicateID(int flightID) throws DAOException {
		try (PreparedStatement ps = prepareWithoutLimits("SELECT F.ID, COUNT(P.FLIGHT_ID) AS POSCNT FROM acars.FLIGHTS F LEFT JOIN acars.POSITIONS P ON (F.ID=P.FLIGHT_ID) WHERE "
			+ "(F.CREATED=(SELECT CREATED FROM acars.FLIGHTS WHERE (ID=?) LIMIT 1)) AND (F.PILOT_ID=(SELECT PILOT_ID FROM acars.FLIGHTS WHERE (ID=?) LIMIT 1)) GROUP BY F.ID "
			+ "ORDER BY POSCNT DESC, F.ID")) {
			ps.setInt(1, flightID);
			ps.setInt(2, flightID);
			
			List<PositionCount> results = new ArrayList<PositionCount>();
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					results.add(new PositionCount(rs.getInt(1), rs.getInt(2)));
			}
			
			return results;
		} catch (SQLException se) {
			throw new DAOException(se);
		}
	}
	
	/**
	 * Searches for a duplicate flight ID created at the same time by the same user.
	 * @param userID the Pilot's database ID
	 * @param createdOn the creation date/time
	 * @return the duplicate flight ID, or zero if none found
	 * @throws DAOException if a JDBC error occurs
	 */
	public List<PositionCount> find(int userID, java.time.Instant createdOn) throws DAOException {
		try (PreparedStatement ps = prepareWithoutLimits("SELECT F.ID, COUNT(P.FLIGHT_ID) AS POSCNT FROM acars.FLIGHTS F LEFT JOIN acars.POSITIONS P ON (F.ID=P.FLIGHT_ID) WHERE "
				+ "(F.CREATED=?) AND (F.PILOT_ID=?) AND (F.PIREP=?) GROUP BY F.ID ORDER BY POSCNT DESC, F.ID LIMIT 1")) {
			ps.setTimestamp(1, createTimestamp(createdOn));
			ps.setInt(2, userID);
			ps.setBoolean(3, false);
			
			List<PositionCount> results = new ArrayList<PositionCount>();
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					results.add(new PositionCount(rs.getInt(1), rs.getInt(2)));
			}
			
			return results;
		} catch (SQLException se) {
			throw new DAOException(se);
		}
	}
}