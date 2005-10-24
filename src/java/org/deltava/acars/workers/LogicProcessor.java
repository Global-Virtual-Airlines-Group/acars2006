package org.deltava.acars.workers;

import java.util.*;
import java.sql.Connection;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.util.*;

import org.deltava.jdbc.*;

import org.deltava.dao.acars.*;
import org.deltava.dao.DAOException;

import org.deltava.util.system.SystemData;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class LogicProcessor extends Worker {

	private static final long CACHE_FLUSH = 30000;

	private ACARSConnectionPool _pool;
	private Map _commands;

	public LogicProcessor() {
		super("Message Processor", LogicProcessor.class);
	}

	public void open() {
		super.open();

		// Initialize commands
		_commands = new HashMap();
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
		log.info("Loaded " + _commands.size() + " commands");
	}
	
	public void close() {
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

		// Get the connection pool
		ConnectionPool pool = (ConnectionPool) SystemData.getObject(SystemData.JDBC_POOL);
		Connection c = null;
		try {
			c = pool.getConnection(true);
			SetPosition dao = new SetPosition(c);

			// Flush the cache
			synchronized (PositionCache.class) {
				for (Iterator i = PositionCache.getAll().iterator(); i.hasNext();) {
					PositionCache.PositionCacheEntry ce = (PositionCache.PositionCacheEntry) i.next();
					try {
						dao.write(ce.getMessage(), ce.getConnectionID(), ce.getFlightID());
						i.remove();
					} catch (DAOException de) {
						log.error("Error writing position - " + de.getMessage(), de);
					}
				}

				dao.release();
				PositionCache.flush();
			}
		} catch (Exception e) {
			log.error("Cannot flush Position Cache - " + e.getMessage());
		} finally {
			pool.release(c);
		}
	}
	
	private void flushMessageCache() {
	   log.debug("Flushing Text Message Cache");
	   
		// Get the connection pool
		ConnectionPool pool = (ConnectionPool) SystemData.getObject(SystemData.JDBC_POOL);
		Connection c = null;
		try {
			c = pool.getConnection(true);
			SetMessage dao = new SetMessage(c);

			// Flush the cache
			synchronized (TextMessageCache.class) {
				for (Iterator i = PositionCache.getAll().iterator(); i.hasNext();) {
					TextMessageCache.TextMessageCacheEntry ce = (TextMessageCache.TextMessageCacheEntry) i.next();
					try {
						dao.write(ce.getMessage(), ce.getConnectionID(), ce.getRecipientID());
						i.remove();
					} catch (DAOException de) {
						log.error("Error writing position - " + de.getMessage(), de);
					}
				}
				
				dao.release();
				PositionCache.flush();
			}
		   
		} catch (Exception e) {
		   log.error("Cannot flush Text Message Cache - " + e.getMessage());
		} finally {
		   pool.release(c);
		}
	}

	private void process(Envelope env) throws Exception {

		// Get the message and start time
		long startTime = System.currentTimeMillis();
		Message msg = (Message) env.getMessage();

		// Check if we can be anonymous
		boolean isAuthenticated = (env.getOwner() != null);
		if (isAuthenticated == msg.isAnonymous()) {
			log.error("Security Exception from " + env.getOwnerID());
			return;
		}

		// Initialize the command context
		CommandContext ctx = new CommandContext(MessageStack.MSG_OUTPUT, _pool, env.getConnectionID());

		// Log the received message and get the command to process it
		log.debug(Message.MSG_TYPES[msg.getType()] + " message from "
				+ Long.toHexString(env.getConnectionID()).toUpperCase());
		ACARSCommand cmd = (ACARSCommand) _commands.get(new Integer(msg.getType()));
		if (cmd != null) {
			cmd.execute(ctx, env);
		} else {
			log.warn("No command for message");
		}

		// Calculate execution time
		long execTime = System.currentTimeMillis() - startTime;
		if (execTime > 5000)
			log.warn(cmd.getClass().getName() + " completed in " + execTime + "ms");
	}

	protected void $run0() {

		// Get the ACARS Connection Pool
		_pool = (ACARSConnectionPool) SystemData.getObject(SystemData.ACARS_POOL);
		log.info("Started");

		// Keep running until we're interrupted
		while (!Thread.currentThread().isInterrupted()) {
		   long startTime = System.currentTimeMillis();
		   
			while (MessageStack.MSG_INPUT.hasNext()) {
				Envelope env = MessageStack.MSG_INPUT.pop();
				try {
					process(env);
				} catch (Exception e) {
					log.error("Error Processing Message from " + env.getOwnerID() + " - " + e.getMessage(), e);
				}
				
				// Don't get bogged down if we're taking too long
				long interval = (System.currentTimeMillis() - startTime);
				if (interval > 1500) {
				   MessageStack.MSG_OUTPUT.wakeup();
				   log.warn("Loop time = " + interval + " ms");
				   startTime = System.currentTimeMillis();
				}
			}
			
			// Notify everyone waiting on the output stack
			MessageStack.MSG_OUTPUT.wakeup();
			
			// Check if we need to flush the position/message caches
			if (PositionCache.isDirty() && (PositionCache.getFlushInterval() > CACHE_FLUSH))
				flushPositionCache();
			else if (TextMessageCache.isDirty() && (TextMessageCache.getFlushInterval() > CACHE_FLUSH))
				flushMessageCache();

			// Wait on the input queue for 5 seconds if we haven't already been interrupted
			if (!Thread.currentThread().isInterrupted()) {
				try {
				   MessageStack.MSG_INPUT.waitForActivity();
				} catch (InterruptedException ie) {
					log.info("Interrupted");
					Thread.currentThread().interrupt();
				}
			}
		}
	}
}