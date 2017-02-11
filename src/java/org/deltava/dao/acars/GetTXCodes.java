// Copyright 2017 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;
import java.util.*;

import org.deltava.acars.beans.TXCode;

import org.deltava.dao.*;

/**
 * A Data Access Object to load active transponder codes.
 * @author Luke
 * @version 7.2
 * @since 7.2
 */

public class GetTXCodes extends DAO {

	/**
	 * Initializes the Data Access Object.
	 * @param c the JDBC connection to use
	 */
	public GetTXCodes(Connection c) {
		super(c);
	}

	/**
	 * Loads active transponder codes.
	 * @return a Map of TXCode beans, indexed by code
	 * @throws DAOException if a JDBC error occurs
	 */
	public Map<Integer, TXCode> getCodes() throws DAOException {
		try {
			prepareStatementWithoutLimits("SELECT PILOT_ID, CREATED, TX FROM acars.FLIGHTS WHERE (END_TIME IS NOT NULL) AND (CREATED>DATE_SUB(NOW(), INTERVAL ? HOUR)) AND (TX<>?)");
			_ps.setInt(1, 16);
			_ps.setInt(2, 2200);
			
			// Execute the query
			try (ResultSet rs = _ps.executeQuery()) {
				Map<Integer, TXCode> results = new HashMap<Integer, TXCode>();
				while (rs.next()) {
					TXCode tx = new TXCode(rs.getInt(3));
					tx.setID(rs.getInt(1));
					tx.setAssignedOn(toInstant(rs.getTimestamp(2)));
					results.put(Integer.valueOf(tx.getCode()), tx);
				}
				
				return results;
			}
		} catch (SQLException se) {
			throw new DAOException(se);
		}
	}
}