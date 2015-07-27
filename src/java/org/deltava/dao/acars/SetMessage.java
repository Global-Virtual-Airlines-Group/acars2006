// Copyright 2005, 2007, 2015 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.dao.acars;

import java.sql.*;

import org.deltava.dao.DAO;
import org.deltava.dao.DAOException;

import org.deltava.acars.message.TextMessage;

/**
 * A Data Access Object to log ACARS messages.
 * @author Luke
 * @version 6.1
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
	 * @param recipientID the message recipient
	 * @throws DAOException if a JDBC error occurs
	 */
	public void write(TextMessage msg, int recipientID) throws DAOException {
		try {
			prepareStatement("INSERT INTO acars.MESSAGES (DATE, AUTHOR, RECIPIENT, BODY) VALUES (DATE_ADD(NOW(), INTERVAL RAND(500)*100000 MICROSECOND) , ?, ?, ?)");
		   	_ps.setQueryTimeout(2);
			_ps.setInt(1, msg.getSender().getID());
			_ps.setInt(2, recipientID);
			_ps.setString(3, msg.getText());
			executeUpdate(1);
		} catch (SQLException se) {
			throw new DAOException(se);
		}
	}
}