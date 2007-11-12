// Copyright 2005, 2006, 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.Message;
import static org.deltava.acars.workers.Worker.*;


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
	private long _msgTime;

	/**
	 * Initializes the Command Context.
	 * @param acp the ACARS Connection Pool
	 * @param env the Message envlope to process
	 * @param status the current Worker Thread status
	 */
	public CommandContext(ACARSConnectionPool acp, Envelope env, WorkerStatus status) {
		super();
		_pool = acp;
		_ac = _pool.get(env.getConnectionID());
		_msgTime = env.getTime();
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
		if (msg == null)
			return;
		
		// Set the original timestamp and message time
		msg.setTime(_msgTime);
		for (Iterator<ACARSConnection> i = _pool.get("*").iterator(); i.hasNext();) {
			ACARSConnection c = i.next();
			if (c.isAuthenticated() && (c.getID() != skipThisConID))
				MSG_OUTPUT.add(new MessageEnvelope(msg, c.getID()));
		}
	}
	
	/**
	 * Sends a message.
	 * @param msg the Message bean to push
	 * @param conID the ID of the Connection to send to
	 */
	public void push(Message msg, long conID) {
		push(msg, conID, false);
	}

	/**
	 * Sends a message.
	 * @param msg the Message bean to push
	 * @param conID the ID of the Connection to send to
	 * @param isCritical TRUE if the response is critical, otherwise FALSE
	 */
	public void push(Message msg, long conID, boolean isCritical) {
		if (msg != null) {
			msg.setTime(_msgTime);
			MessageEnvelope env = new MessageEnvelope(msg, conID);
			env.setCritical(isCritical);
			MSG_OUTPUT.add(env);
		}
	}
	
	/**
	 * Updates the worker thread's status message.
	 * @param msg the new status message
	 */
	public void setMessage(String msg) {
		_status.setMessage(msg);
	}
}