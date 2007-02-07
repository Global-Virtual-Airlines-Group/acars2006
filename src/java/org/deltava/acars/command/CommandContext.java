// Copyright 2005, 2006, 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.Message;

import org.deltava.acars.workers.WorkerStatus;

import org.deltava.jdbc.*;

/**
 * The ACARS command context object.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class CommandContext extends ConnectionContext {

	private ACARSConnectionPool _pool;
	private ACARSConnection _ac;
	
	private WorkerStatus _status;

	/**
	 * Initializes the Command Context.
	 * @param acp the ACARS Connection Pool
	 * @param conID the Connection ID
	 * @param status the current Worker Thread status
	 */
	public CommandContext(ACARSConnectionPool acp, long conID, WorkerStatus status) {
		super();
		_pool = acp;
		_ac = _pool.get(conID);
		_status = status;
	}
	
	/**
	 * Returns the current ACARS Connection.
	 * @return the ACARSConnection
	 */
	public ACARSConnection getACARSConnection() {
		return _ac;
	}

	/**
	 * Returns all ACARS Connections matching a particular Pilot ID.
	 * @param pilotID the pilot ID
	 * @return a Collection of ACARSConnection beans
	 * @see ACARSConnectionPool#get(String)
	 */
	public Collection<ACARSConnection> getACARSConnections(String pilotID) {
		return _pool.get(pilotID);
	}

	/**
	 * Returns the ACARS Connection Pool.
	 * @return the connection pool
	 */
	ACARSConnectionPool getACARSConnectionPool() {
		return _pool;
	}

	/**
	 * Sends a message to all <b>authenticated</b> ACARS connections.
	 * @param msg the Message to send
	 * @param skipThisConID the ID of a Connection to not send to (usually the sender)
	 */
	public void pushAll(Message msg, long skipThisConID) {
		for (Iterator<ACARSConnection> i = _pool.getAll().iterator(); i.hasNext();) {
			ACARSConnection c = i.next();
			if (c.isAuthenticated() && (c.getID() != skipThisConID))
				MessageStack.MSG_OUTPUT.push(new MessageEnvelope(msg, c.getID()));
		}
	}

	/**
	 * Sends a message.
	 * @param msg the Message bean to push
	 * @param conID the ID of the Connection to send to
	 */
	public void push(Message msg, long conID) {
		if (msg != null)
			MessageStack.MSG_OUTPUT.push(new MessageEnvelope(msg, conID));
	}
	
	/**
	 * Updates the worker thread's status message.
	 * @param msg the new status message
	 */
	public void setMessage(String msg) {
		_status.setMessage(msg);
	}
}