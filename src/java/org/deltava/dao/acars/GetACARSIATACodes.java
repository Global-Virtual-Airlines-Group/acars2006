// Copyright 2013, 2014, 2016, 2017, 2018, 2019, 2022 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;
import java.util.*;
import java.sql.Connection;

import org.deltava.beans.acars.IATACodes;
import org.deltava.beans.flight.*;

import org.deltava.dao.*;

import org.deltava.util.cache.*;

/**
 * A Data Access Object to fetch IATA codes used by aircraft.
 * @author Luke
 * @version 10.3
 * @since 5.1
 */

public class GetACARSIATACodes extends DAO {
	
	private static final Cache<CacheableMap<String, IATACodes>> _cache = CacheManager.getMap(String.class, IATACodes.class, "IATACodes");
	
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
		
		// Check the cache
		String dbName = formatDBName(db);
		CacheableMap<String, IATACodes> results = _cache.get(dbName);
		if (results != null)
			return new LinkedHashMap<String, IATACodes>(results);
		
		// Build the SQL statement
		StringBuilder sqlBuf = new StringBuilder("SELECT P.EQTYPE, AM.CODE, COUNT(AM.ID) AS CNT FROM ");
		sqlBuf.append(dbName);
		sqlBuf.append(".PIREPS P, ");
		sqlBuf.append(dbName);
		sqlBuf.append(".ACARS_METADATA AM WHERE (P.ID=AM.ID) AND (P.STATUS=?) AND (LENGTH(AM.CODE)>2) GROUP BY P.EQTYPE, AM.CODE HAVING (CNT>5) ORDER BY P.EQTYPE, CNT DESC"); 
		
		try (PreparedStatement ps = prepareWithoutLimits(sqlBuf.toString())) {
			ps.setInt(1, FlightStatus.OK.ordinal());
			results = new CacheableMap<String, IATACodes>(dbName);
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
			
			_cache.add(results);
			return new LinkedHashMap<String, IATACodes>(results);
		} catch (SQLException se) {
			throw new DAOException(se);
		}
	}
}