// Copyright 2007, 2008, 2012, 2017 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;
import java.util.*;

import org.deltava.dao.*;
import org.deltava.beans.navdata.*;

import org.deltava.acars.message.dispatch.FlightDataMessage;

/**
 * A Data Access Object to write routes into the database.
 * @author Luke
 * @version 8.0
 * @since 2.0
 */

public class SetRoute extends DAO {

	/**
	 * Initializes the Data Access Object.
	 * @param c the JDBC connection to use
	 */
	public SetRoute(Connection c) {
		super(c);
	}

	/**
	 * Saves a Flight route to the database.
	 * @param msg the FlightDataMessage
	 * @param build the client build number
	 * @throws DAOException if a JDBC error occurs
	 */
	public void save(FlightDataMessage msg, int build) throws DAOException {
		try {
			startTransaction();

			// Write the data
			prepareStatementWithoutLimits("INSERT INTO acars.ROUTES (AUTHOR, AIRLINE, AIRPORT_D, AIRPORT_A, AIRPORT_L, CREATEDON, LASTUSED, USED, ALTITUDE, SID, STAR, BUILD, "
				+ "REMARKS, ROUTE) VALUES (?, ?, ?, ?, ?, NOW(), NOW(), 1, ?, ?, ?, ?, ?, ?)");
			_ps.setInt(1, msg.getSender().getID());
			_ps.setString(2, msg.getAirline().getCode());
			_ps.setString(3, msg.getAirportD().getIATA());
			_ps.setString(4, msg.getAirportA().getIATA());
			_ps.setString(5, (msg.getAirportL() == null) ? null : msg.getAirportL().getIATA());
			_ps.setString(6, msg.getCruiseAltitude());
			_ps.setString(7, msg.getSID());
			_ps.setString(8, msg.getSTAR());
			_ps.setInt(9, build);
			_ps.setString(10, msg.getComments());
			_ps.setString(11, msg.getRoute());

			// Save the data
			executeUpdate(1);
			if (msg.getRouteID() == 0)
				msg.setRouteID(getNewID());

			// Save the waypoints
			int seq = -1;
			prepareStatementWithoutLimits("INSERT INTO acars.ROUTE_WP (ID, SEQ, CODE, ITEMTYPE, LATITUDE, LONGITUDE, AIRWAY, REGION) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
			_ps.setInt(1, msg.getRouteID());
			for (Iterator<NavigationDataBean> i = msg.getWaypoints().iterator(); i.hasNext();) {
				NavigationDataBean nd = i.next();
				if (!nd.isInTerminalRoute()) {
					_ps.setInt(2, ++seq);
					_ps.setString(3, nd.getCode());
					_ps.setInt(4, nd.getType().ordinal());
					_ps.setDouble(5, nd.getLatitude());
					_ps.setDouble(6, nd.getLongitude());
					_ps.setString(7, nd.getAirway());
					_ps.setString(8, nd.getRegion());
					_ps.addBatch();
				}
			}

			// Write and commit
			executeBatchUpdate(1, msg.getWaypoints().size());
			commitTransaction();
		} catch (SQLException se) {
			rollbackTransaction();
			throw new DAOException(se);
		}
	}
}