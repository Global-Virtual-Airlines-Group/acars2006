// Copyright 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;
import java.sql.Connection;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.Message;

import org.deltava.acars.workers.WorkerStatus;

import org.deltava.jdbc.*;

import org.deltava.util.system.SystemData;

/**
 * The ACARS command context object.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class CommandContext {

	private Connection _con;

	private ACARSConnectionPool _pool;
	private ACARSConnection _ac;
	
	private WorkerStatus _status;

	/**
	 * Initializes the Command Context.
	 */
	public CommandContext(ACARSConnectionPool acp, long conID, WorkerStatus status) {
		super();
		_pool = acp;
		_ac = _pool.get(conID);
		_status = status;
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

	public Collection<ACARSConnection> getACARSConnections(String pilotID) {
		return _pool.get(pilotID);
	}

	ACARSConnectionPool getACARSConnectionPool() {
		return _pool;
	}

	public void pushAll(Message msg, long skipThisConID) {
		for (Iterator<ACARSConnection> i = _pool.getAll().iterator(); i.hasNext();) {
			ACARSConnection c = i.next();
			if (c.isAuthenticated() && (c.getID() != skipThisConID))
				MessageStack.MSG_OUTPUT.push(new MessageEnvelope(msg, c.getID()));
		}
	}

	public void push(Message msg, long conID) {
		MessageStack.MSG_OUTPUT.push(new MessageEnvelope(msg, conID));
	}
	
	public void setMessage(String msg) {
		_status.setMessage(msg);
	}
}