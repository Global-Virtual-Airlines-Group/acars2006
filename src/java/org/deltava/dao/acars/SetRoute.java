// Copyright 2007, 2008, 2012, 2017, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;

import org.deltava.dao.*;
import org.deltava.beans.navdata.*;

import org.deltava.acars.message.dispatch.FlightDataMessage;

/**
 * A Data Access Object to write routes into the database.
 * @author Luke
 * @version 9.0
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
			try (PreparedStatement ps = prepareWithoutLimits("INSERT INTO acars.ROUTES (AUTHOR, AIRLINE, AIRPORT_D, AIRPORT_A, AIRPORT_L, CREATEDON, LASTUSED, USED, ALTITUDE, SID, STAR, BUILD, "
					+ "REMARKS, ROUTE) VALUES (?, ?, ?, ?, ?, NOW(), NOW(), 1, ?, ?, ?, ?, ?, ?)")) {
				ps.setInt(1, msg.getSender().getID());
				ps.setString(2, msg.getAirline().getCode());
				ps.setString(3, msg.getAirportD().getIATA());
				ps.setString(4, msg.getAirportA().getIATA());
				ps.setString(5, (msg.getAirportL() == null) ? null : msg.getAirportL().getIATA());
				ps.setString(6, msg.getCruiseAltitude());
				ps.setString(7, msg.getSID());
				ps.setString(8, msg.getSTAR());
				ps.setInt(9, build);
				ps.setString(10, msg.getComments());
				ps.setString(11, msg.getRoute());
				executeUpdate(ps, 1);
			}
			
			// Get the ID
			if (msg.getRouteID() == 0) msg.setRouteID(getNewID());

			// Save the waypoints
			try (PreparedStatement ps = prepareWithoutLimits("INSERT INTO acars.ROUTE_WP (ID, SEQ, CODE, ITEMTYPE, LATITUDE, LONGITUDE, AIRWAY, REGION) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
				ps.setInt(1, msg.getRouteID()); int seq = -1;
				for (NavigationDataBean nd : msg.getWaypoints()) {
					if (nd.isInTerminalRoute()) continue;
					ps.setInt(2, ++seq);
					ps.setString(3, nd.getCode());
					ps.setInt(4, nd.getType().ordinal());
					ps.setDouble(5, nd.getLatitude());
					ps.setDouble(6, nd.getLongitude());
					ps.setString(7, nd.getAirway());
					ps.setString(8, nd.getRegion());
					ps.addBatch();
				}

				executeUpdate(ps, 1, msg.getWaypoints().size());
			}

			commitTransaction();
		} catch (SQLException se) {
			rollbackTransaction();
			throw new DAOException(se);
		}
	}
}