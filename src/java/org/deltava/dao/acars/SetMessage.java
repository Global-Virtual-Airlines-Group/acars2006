// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;

import org.deltava.dao.DAO;
import org.deltava.dao.DAOException;

import org.deltava.beans.Pilot;
import org.deltava.acars.message.TextMessage;

/**
 * A Data Access Object to log ACARS messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class SetMessage extends DAO {

	/**
	 * Initialize the Data Access Object.
	 * @param c the JDBC connection to use
	 */
	public SetMessage(Connection c) {
		super(c);
	}

	/**
	 * Writes a chat message to the database.
	 * @param msg the message
	 * @param conID the Connection ID
	 * @param recipient the message recipient
	 * @throws DAOException if a JDBC error occurs
	 */
	public void write(TextMessage msg, long conID, Pilot recipient) throws DAOException {
		
		try {
			prepareStatement("INSERT INTO acars.MESSAGES (CON_ID, ID, DATE, AUTHOR, RECIPIENT, BODY) VALUES (?, ?, ?, ?, ?, ?)");
			_ps.setLong(1, conID);
			_ps.setLong(2, msg.getID());
			_ps.setTimestamp(3, new Timestamp(msg.getTime()));
			_ps.setInt(4, msg.getSender().getID());
			_ps.setInt(5, (recipient == null) ? 0 : recipient.getID());
			_ps.setString(6, msg.getText());
			
			// Update the database
			executeUpdate(1);
		} catch (SQLException se) {
			throw new DAOException(se);
		}
	}
}