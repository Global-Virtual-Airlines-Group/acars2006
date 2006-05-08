// Copyright 2004, 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.*;
import java.sql.Connection;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.util.*;

import org.deltava.beans.acars.CommandStats;

import org.deltava.jdbc.*;

import org.deltava.dao.acars.*;
import org.deltava.dao.DAOException;

import org.deltava.util.system.SystemData;

/**
 * An ACARS Worker thread to process messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class LogicProcessor extends Worker {

	private static final long CACHE_FLUSH = 30000;

	private ACARSConnectionPool _pool;
	private static Map<Integer, ACARSCommand> _commands;
	
	public LogicProcessor(int threadID) {
		super("Message Processor #" + threadID, LogicProcessor.class.getName() + "-" + threadID);
	}

	public synchronized void open() {
		super.open();
		
		// Get the ACARS Connection Pool
		_pool = (ACARSConnectionPool) SystemData.getObject(SystemData.ACARS_POOL);

		// Initialize commands
		if (_commands == null) {
			_commands = new HashMap<Integer, ACARSCommand>();
			_commands.put(new Integer(Message.MSG_ACK), new DummyCommand());
			_commands.put(new Integer(Message.MSG_PING), new AcknowledgeCommand("ping"));
			_commands.put(new Integer(Message.MSG_POSITION), new PositionCommand());
			_commands.put(new Integer(Message.MSG_TEXT), new TextMessageCommand());
			_commands.put(new Integer(Message.MSG_AUTH), new AuthenticateCommand());
			_commands.put(new Integer(Message.MSG_DATAREQ), new DataCommand());
			_commands.put(new Integer(Message.MSG_INFO), new InfoCommand());
			_commands.put(new Integer(Message.MSG_ENDFLIGHT), new EndFlightCommand());
			_commands.put(new Integer(Message.MSG_QUIT), new QuitCommand());
			_commands.put(new Integer(Message.MSG_DIAG), new DiagnosticCommand());
			_commands.put(new Integer(Message.MSG_PIREP), new FilePIREPCommand());
			_commands.put(new Integer(Message.MSG_ERROR), new ErrorCommand());
			_commands.put(new Integer(Message.MSG_DIAG), new DiagnosticCommand());
			_commands.put(new Integer(Message.MSG_DISPATCH), new DispatchCommand());
			log.info("Loaded " + _commands.size() + " commands");
		}
	}

	public synchronized void close() {
		if (PositionCache.isDirty()) {
			log.info("Position Cache is Dirty - flushing");
			flushPositionCache();
		}

		if (TextMessageCache.isDirty()) {
			log.info("Text Message Cache is Dirty - flushing");
			flushMessageCache();
		}

		super.close();
	}

	private void flushPositionCache() {
		log.debug("Flushing Position Cache");
		_status.setMessage("Flushing Position Cache");

		// Get the connection pool
		ConnectionPool pool = (ConnectionPool) SystemData.getObject(SystemData.JDBC_POOL);
		Connection c = null;
		try {
			c = pool.getConnection(true);
			SetPosition dao = new SetPosition(c);

			// Flush the cache
			PositionCache.PositionCacheEntry ce = PositionCache.pop();
			while (ce != null) {
				try {
					dao.write(ce.getMessage(), ce.getConnectionID(), ce.getFlightID());
				} catch (DAOException de) {
					log.error("Error writing position - " + de.getMessage(), de);
				}
				
				ce = PositionCache.pop();
			}
			
			dao.release();
			PositionCache.flush();
		} catch (Exception e) {
			log.error("Cannot flush Position Cache - " + e.getMessage());
		} finally {
			pool.release(c);
		}
	}

	private void flushMessageCache() {
		log.debug("Flushing Text Message Cache");
		_status.setMessage("Flushing Text Message Cache");

		// Get the connection pool
		ConnectionPool pool = (ConnectionPool) SystemData.getObject(SystemData.JDBC_POOL);
		Connection c = null;
		try {
			c = pool.getConnection(true);
			SetMessage dao = new SetMessage(c);

			// Flush the cache
			TextMessageCache.TextMessageCacheEntry ce = TextMessageCache.pop();
			while (ce != null) {
				try {
					dao.write(ce.getMessage(), ce.getConnectionID(), ce.getRecipientID());
				} catch (DAOException de) {
					log.error("Error writing position - " + de.getMessage(), de);
				}
				
				ce = TextMessageCache.pop();
			}
			
			dao.release();
			TextMessageCache.flush();
		} catch (Exception e) {
			log.error("Cannot flush Text Message Cache - " + e.getMessage());
		} finally {
			pool.release(c);
		}
	}

	private void process(Envelope env) throws Exception {
		if (env == null)
			return;

		// Get the message and start time
		long startTime = System.currentTimeMillis();
		Message msg = (Message) env.getMessage();
		_status.setMessage("Processing " + Message.MSG_TYPES[msg.getType()] + " message from " + env.getOwnerID());

		// Check if we can be anonymous
		boolean isAuthenticated = (env.getOwner() != null);
		if (isAuthenticated == msg.isAnonymous()) {
			log.error("Security Exception from " + env.getOwnerID());
			return;
		}

		// Initialize the command context
		CommandContext ctx = new CommandContext(_pool, env.getConnectionID(), _status);

		// Log the received message and get the command to process it
		log.debug(Message.MSG_TYPES[msg.getType()] + " message from " + env.getOwnerID());
		ACARSCommand cmd = _commands.get(new Integer(msg.getType()));
		if (cmd != null) {
			cmd.execute(ctx, env);
			
			// Calculate and log execution time
			long execTime = System.currentTimeMillis() - startTime;
			CommandStats.log(cmd.getClass(), execTime);
			if (execTime > cmd.getMaxExecTime())
				log.warn(cmd.getClass().getName() + " completed in " + execTime + "ms");
		} else {
			log.warn("No command for " + Message.MSG_TYPES[msg.getType()] + " message");
		}
	}

	protected void $run0() {
		log.info("Started");

		// Keep running until we're interrupted
		while (!Thread.currentThread().isInterrupted()) {
			long startTime = System.currentTimeMillis();

			while (MessageStack.MSG_INPUT.hasNext()) {
				Envelope env = MessageStack.MSG_INPUT.pop();
				try {
					_status.execute();
					process(env);
					_status.complete();
				} catch (Exception e) {
					log.error("Error Processing Message from " + env.getOwnerID() + " - " + e.getMessage(), e);
				}

				// Don't get bogged down if we're taking too long
				long interval = (System.currentTimeMillis() - startTime);
				if (interval > 1750) {
					MessageStack.MSG_OUTPUT.wakeup();
					startTime = System.currentTimeMillis();
				}
			}

			// Notify everyone waiting on the output stack
			MessageStack.MSG_OUTPUT.wakeup();

			// Check if we need to flush the position/message caches
			
			_status.setMessage("Checking Message/Position Caches");
			synchronized (LogicProcessor.class) {
				_status.execute();
				if (PositionCache.isDirty() && (PositionCache.getFlushInterval() > CACHE_FLUSH))
					flushPositionCache();
				else if (TextMessageCache.isDirty() && (TextMessageCache.getFlushInterval() > CACHE_FLUSH))
					flushMessageCache();
				_status.complete();
			}

			// Wait on the input queue for 5 seconds if we haven't already been interrupted
			_status.setMessage("Idle");
			try {
				MessageStack.MSG_INPUT.waitForActivity();
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
		}
		
		log.info("Interrupted");
	}
}