package org.deltava.acars.workers;

import java.util.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;

import org.deltava.util.system.SystemData;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class LogicProcessor extends Worker {

	private ACARSConnectionPool _pool;
	private Map _commands;

	public LogicProcessor() {
		super("Message Processor", LogicProcessor.class);
	}

	public void open() {
		super.open();

		// Initialize commands
		_commands = new HashMap();
		_commands.put(new Integer(Message.MSG_ACK), new AcknowledgeCommand());
		_commands.put(new Integer(Message.MSG_PING), new AcknowledgeCommand());
		_commands.put(new Integer(Message.MSG_POSITION), new PositionCommand());
		_commands.put(new Integer(Message.MSG_TEXT), new TextMessageCommand());
		_commands.put(new Integer(Message.MSG_AUTH), new AuthenticateCommand());
		_commands.put(new Integer(Message.MSG_DATAREQ), new DataCommand());
		_commands.put(new Integer(Message.MSG_INFO), new InfoCommand());
		_commands.put(new Integer(Message.MSG_ENDFLIGHT), new EndFlightCommand());
		_commands.put(new Integer(Message.MSG_QUIT), new QuitCommand());
		_commands.put(new Integer(Message.MSG_DIAG), new DiagnosticCommand());
		_commands.put(new Integer(Message.MSG_PIREP), new FilePIREPCommand());
		log.info("Loaded " + _commands.size() + " commands");
	}

	private void process(Envelope env) throws Exception {
		
		// Get the message
		Message msg = (Message) env.getMessage();
		
		// Check if we can be anonymous
		boolean isAuthenticated = (env.getOwner() != null);
		if (isAuthenticated == msg.isAnonymous()) {
			log.error("Security Exception from " + env.getOwnerID());
			return;
		}

		// Initialize the command context
		CommandContext ctx = new CommandContext(_outStack, _pool, env.getConnectionID());

		// Log the received message and get the command to process it
		log.debug(Message.MSG_TYPES[msg.getType()] + " message from " + Long.toHexString(env.getConnectionID()).toUpperCase());
		ACARSCommand cmd = (ACARSCommand) _commands.get(new Integer(msg.getType()));
		if (cmd != null) {
			cmd.execute(ctx, env);
		} else {
			log.warn("No command for message");
		}
	}

	protected void $run0() {

		// Get the ACARS Connection Pool
		_pool = (ACARSConnectionPool) SystemData.getObject(SystemData.ACARS_POOL);
		log.info("Started");

		// Keep running until we're interrupted
		while (!Thread.currentThread().isInterrupted()) {
			while (_inStack.hasNext()) {
				Envelope env = _inStack.pop();
				try {
					process(env);
				} catch (Exception e) {
					log.error("Error Processing Message - " + e.getMessage(), e);
				}
			}

			// Notify everyone waiting on the output stack
			_outStack.wakeup();

			// Wait on the input queue for 5 seconds if we haven't already been interrupted
			if (!Thread.currentThread().isInterrupted()) {
				try {
					synchronized (_inStack) {
						_inStack.wait(5000);
					}
				} catch (InterruptedException ie) {
					log.info("Interrupted");
					Thread.currentThread().interrupt();
				}
			}
		}
	}
}