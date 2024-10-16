// Copyright 2013, 2014, 2016, 2017, 2018, 2019, 2022, 2024 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;
import java.util.*;
import java.sql.Connection;

import org.deltava.beans.acars.IATACodes;
import org.deltava.beans.flight.*;

import org.deltava.dao.*;

/**
 * A Data Access Object to fetch IATA codes used by aircraft.
 * @author Luke
 * @version 11.3
 * @since 5.1
 */

@Deprecated
public class GetACARSIATACodes extends DAO {
	
	/**
	 * Initializes the Data Access Object.
	 * @param c the JDBC connection to use
	 */
	public GetACARSIATACodes(Connection c) {
		super(c);
	}

	/**
	 * Returns all IATA codes for all equipment types.
	 * @param db the database name
	 * @return a Map of Collections of IATA codes, keyed by equipment type name
	 * @throws DAOException if a JDBC error occurs
	 */
	public Map<String, IATACodes> getAll(String db) throws DAOException {
		
		// Build the SQL statement
		String dbName = formatDBName(db);
		StringBuilder sqlBuf = new StringBuilder("SELECT P.EQTYPE, AM.CODE, COUNT(AM.ID) AS CNT FROM ");
		sqlBuf.append(dbName);
		sqlBuf.append(".PIREPS P, ");
		sqlBuf.append(dbName);
		sqlBuf.append(".ACARS_METADATA AM WHERE (P.ID=AM.ID) AND (P.STATUS=?) AND (LENGTH(AM.CODE)>2) GROUP BY P.EQTYPE, AM.CODE HAVING (CNT>5) ORDER BY P.EQTYPE, CNT DESC"); 
		
		Map<String, IATACodes> results = new LinkedHashMap<String, IATACodes>();
		try (PreparedStatement ps = prepareWithoutLimits(sqlBuf.toString())) {
			ps.setInt(1, FlightStatus.OK.ordinal());
			try (ResultSet rs = ps.executeQuery()) {
				int max = 0; IATACodes c = null;
				while (rs.next()) {
					String code = rs.getString(1);
					String iata = rs.getString(2);
					if (iata.length() > 5) continue;
					if ((c == null) || !code.equals(c.getEquipmentType())) {
						max = 0;
						if (c != null)
							results.put(c.getEquipmentType(), c);
						
						c = new IATACodes(code);
					}
					
					int cnt = rs.getInt(3);
					max = Math.max(max, cnt);
					if (cnt > (max / 5))
						c.put(iata, Integer.valueOf(cnt));
				}
			}
			
			return new LinkedHashMap<String, IATACodes>(results);
		} catch (SQLException se) {
			throw new DAOException(se);
		}
	}
}