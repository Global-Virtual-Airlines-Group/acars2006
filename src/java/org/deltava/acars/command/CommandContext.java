// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;
import java.sql.Connection;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.Message;
import org.deltava.jdbc.*;

import org.deltava.util.system.SystemData;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class CommandContext {

	private MessageStack _outStack;
	private Connection _con;

	private ACARSConnectionPool _pool;
	private ACARSConnection _ac;

	/**
	 * Initializes the Command Context.
	 */
	public CommandContext(MessageStack stack, ACARSConnectionPool acp, long conID) {
		super();
		_outStack = stack;
		_pool = acp;
		_ac = _pool.get(conID);
	}
	
	public Connection getConnection() throws ConnectionPoolException {
	   return getConnection(false);
	}

	public Connection getConnection(boolean isSystem) throws ConnectionPoolException {
		if (_con != null)
			throw new IllegalStateException("JDBC Connection already reserved");

		// Get the connection pool
		ConnectionPool cp = (ConnectionPool) SystemData.getObject(SystemData.JDBC_POOL);
		_con = cp.getConnection(isSystem);
		return _con;
	}

	public void release() {
		if (_con != null) {
			ConnectionPool cp = (ConnectionPool) SystemData.getObject(SystemData.JDBC_POOL);
			cp.release(_con);
			_con = null;
		}
	}

	public ACARSConnection getACARSConnection() {
		return _ac;
	}

	public Collection getACARSConnections(String pilotID) {
		return _pool.get(pilotID);
	}

	ACARSConnectionPool getACARSConnectionPool() {
		return _pool;
	}

	public void pushAll(Message msg, long skipThisConID) {
		for (Iterator i = _pool.getAll().iterator(); i.hasNext();) {
			ACARSConnection c = (ACARSConnection) i.next();
			if (c.isAuthenticated() && (c.getID() != skipThisConID)) {
				Envelope env = new Envelope(msg, c.getID());
				_outStack.push(env);
			}
		}
	}

	public void push(Message msg, long conID) {
		_outStack.push(new Envelope(msg, conID));
	}
}