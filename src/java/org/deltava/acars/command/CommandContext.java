// Copyright 2005, 2006, 2007, 2009, 2010, 2011, 2019, 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import org.deltava.beans.Pilot;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.Message;

import static org.deltava.acars.workers.Worker.*;

import org.deltava.jdbc.*;
import org.deltava.util.StringUtils;
import org.gvagroup.ipc.WorkerStatus;

/**
 * The ACARS command context object.
 * @author Luke
 * @version 10.0
 * @since 1.0
 */

public class CommandContext extends ConnectionContext {

	private final ACARSConnectionPool _pool;
	private final ACARSConnection _ac;
	
	private final WorkerStatus _status;
	private final long _msgTime;
	private long _backEndTime;

	/**
	 * Initializes the Command Context.
	 * @param acp the ACARS Connection Pool
	 * @param env the Message envlope to process
	 * @param status the current Worker Thread status
	 */
	public CommandContext(ACARSConnectionPool acp, Envelope<Message> env, WorkerStatus status) {
		super();
		_pool = acp;
		_ac = _pool.get(env.getConnectionID());
		_msgTime = env.getTime();
		_status = status;
		if ((_ac != null) && _ac.isAuthenticated())
			setDB(_ac.getUserData().getDB());
	}
	
	/**
	 * Returns the current ACARS Connection.
	 * @return the ACARSConnection
	 */
	public ACARSConnection getACARSConnection() {
		return _ac;
	}
	
	/**
	 * Returns back-end usage time from the parent {@link ConnectionContext}
	 * @return the usage time in milliseconds
	 */
	public long getBackEndTime() {
		return _backEndTime;
	}

	/**
	 * Returns an ACARS Connection for a particular Pilot ID.
	 * @param pilotID the pilot ID
	 * @return an ACARSConnection bean, or null if not found
	 * @see ACARSConnectionPool#get(String)
	 */
	public ACARSConnection getACARSConnection(String pilotID) {
		return StringUtils.isEmpty(pilotID) ? null : _pool.get(pilotID);
	}
	
	/**
	 * Returns a particular ACARS Connection.
	 * @param cid the connection ID
	 * @return an ACARSConnection bean, or null if not found
	 * @see ACARSConnectionPool#get(long)
	 */
	public ACARSConnection getACARSConnection(long cid) {
		return _pool.get(cid);
	}
	
	/**
	 * Returns the ACARS Connection Pool.
	 * @return the connection pool
	 */
	public ACARSConnectionPool getACARSConnectionPool() {
		return _pool;
	}
	
	/**
	 * Returns the Pilot associated with this ACARS Connection. 
	 * @return a Pilot bean, or null
	 */
	public Pilot getUser() {
		return _ac.getUser();
	}

	/**
	 * Sends a message to all <b>authenticated</b> ACARS connections.
	 * @param msg the Message to send
	 * @param skipThisConID the ID of a Connection to not send to (usually the sender)
	 */
	public void pushAll(Message msg, long skipThisConID) {
		if (msg == null) return;
		
		// Set the original timestamp and message time
		msg.setTime(_msgTime);
		for (ACARSConnection c : _pool.getAll()) {
			if (c.isAuthenticated() && (c.getID() != skipThisConID) && (c.getProtocolVersion() >= msg.getProtocolVersion()))
				MSG_OUTPUT.add(new MessageEnvelope(msg, c.getID()));
		}
	}
	
	/**
	 * Sends a message to all connected dispatchers.
	 * @param msg the Message to send
	 * @param skipThisConID the ID of a Connection to not send to (usually the sender)
	 */
	public void pushDispatch(Message msg, long skipThisConID) {
		if (msg == null) return;
		
		// Set the original timestamp and message time
		msg.setTime(_msgTime);
		for (ACARSConnection c : _pool.getAll()) {
			if (c.getIsDispatch() && (c.getID() != skipThisConID) && (c.getProtocolVersion() >= msg.getProtocolVersion()))
				MSG_OUTPUT.add(new MessageEnvelope(msg, c.getID()));
		}
	}
	
	/**
	 * Sends a message to all connected users with voice.
	 * @param msg the Message to send
	 * @param skipThisConID the ID of a Connection to not send to (usually the sender)
	 */
	public void pushVoice(Message msg, long skipThisConID) {
		if (msg == null) return;
		
		// Set the original timestamp and message time
		msg.setTime(_msgTime);
		for (ACARSConnection c : _pool.getAll()) {
			if (c.isVoiceEnabled() && (c.getID() != skipThisConID))
				MSG_OUTPUT.add(new MessageEnvelope(msg, c.getID()));
		}
	}
	
	/**
	 * Pushes a message back to this context's ACARS Connection. 
	 * @param msg a Message
	 */
	public void push(Message msg) {
		push(msg, _ac.getID(), false);
	}
	
	/**
	 * Sends a message.
	 * @param msg the Message bean to push
	 * @param conID the ID of the Connection to send to
	 * @param isCritical TRUE if the response is critical, otherwise FALSE
	 */
	public void push(Message msg, long conID, boolean isCritical) {
		if (msg == null) return;
		msg.setTime(_msgTime);
		MessageEnvelope env = new MessageEnvelope(msg, conID);
		env.setCritical(isCritical);
		MSG_OUTPUT.add(env);
	}
	
	/**
	 * Updates the worker thread's status message.
	 * @param msg the new status message
	 */
	public void setMessage(String msg) {
		_status.setMessage(msg);
	}
	
	@Override
	public long release() {
		_backEndTime = super.release();
		return _backEndTime;
	}
}