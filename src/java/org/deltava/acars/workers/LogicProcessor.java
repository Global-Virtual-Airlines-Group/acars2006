// Copyright 2004, 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.command.data.*;
import org.deltava.acars.message.*;

import org.deltava.beans.acars.CommandStats;

/**
 * An ACARS Worker thread to process messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class LogicProcessor extends Worker {

	private static Map<Integer, ACARSCommand> _commands;
	private static Map<Integer, DataCommand> _dataCommands;

	/**
	 * Initializes the Worker.
	 * @param threadID the worker instance ID
	 */
	public LogicProcessor(int threadID) {
		super("Message Processor #" + threadID, LogicProcessor.class.getName() + "-" + threadID);
	}

	/**
	 * Initializes the Command map.
	 * @see Worker#open()
	 */
	public synchronized void open() {
		super.open();

		// Initialize commands
		if (_commands == null) {
			_commands = new HashMap<Integer, ACARSCommand>();
			_commands.put(new Integer(Message.MSG_ACK), new DummyCommand());
			_commands.put(new Integer(Message.MSG_PING), new AcknowledgeCommand("ping"));
			_commands.put(new Integer(Message.MSG_POSITION), new PositionCommand());
			_commands.put(new Integer(Message.MSG_TEXT), new TextMessageCommand());
			_commands.put(new Integer(Message.MSG_AUTH), new AuthenticateCommand());
			_commands.put(new Integer(Message.MSG_INFO), new InfoCommand());
			_commands.put(new Integer(Message.MSG_ENDFLIGHT), new EndFlightCommand());
			_commands.put(new Integer(Message.MSG_QUIT), new QuitCommand());
			_commands.put(new Integer(Message.MSG_PIREP), new FilePIREPCommand());
			_commands.put(new Integer(Message.MSG_ERROR), new ErrorCommand());
			_commands.put(new Integer(Message.MSG_DIAG), new DiagnosticCommand());
			_commands.put(new Integer(Message.MSG_DISPATCH), new DispatchCommand());
		}

		// Initialize data commands
		if (_dataCommands == null) {
			_dataCommands = new HashMap<Integer, DataCommand>();
			_dataCommands.put(new Integer(DataMessage.REQ_BUSY), new BusyCommand());
			_dataCommands.put(new Integer(DataMessage.REQ_CHARTS), new ChartsCommand());
			_dataCommands.put(new Integer(DataMessage.REQ_DRAFTPIREP), new DraftFlightCommand());
			_dataCommands.put(new Integer(DataMessage.REQ_USRLIST), new UserListCommand());
			_dataCommands.put(new Integer(DataMessage.REQ_ALLIST), new AirlineListCommand());
			_dataCommands.put(new Integer(DataMessage.REQ_APLIST), new AirportListCommand());
			_dataCommands.put(new Integer(DataMessage.REQ_EQLIST), new EquipmentListCommand());
			_dataCommands.put(new Integer(DataMessage.REQ_SCHED), new ScheduleInfoCommand());
			_dataCommands.put(new Integer(DataMessage.REQ_NAVAIDINFO), new NavaidCommand());
			_dataCommands.put(new Integer(DataMessage.REQ_TS2SERVERS), new TS2ServerListCommand());
			_dataCommands.put(new Integer(DataMessage.REQ_PVTVOX), new PrivateVoiceCommand());
			_dataCommands.put(new Integer(DataMessage.REQ_ATCINFO), new ATCInfoCommand());
		}

		log.info("Loaded " + (_commands.size() + _dataCommands.size()) + " commands");
	}

	private void process(MessageEnvelope env) throws Exception {
		if (env == null)
			return;

		// Get the message and start time
		long startTime = System.currentTimeMillis();
		Message msg = env.getMessage();
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
		ACARSCommand cmd = null;
		if (msg.getType() == Message.MSG_DATAREQ) {
			DataRequestMessage reqmsg = (DataRequestMessage) msg;
			cmd = _dataCommands.get(new Integer(reqmsg.getRequestType()));
			if (cmd == null) {
				log.warn("No Data Command for " + DataMessage.REQ_TYPES[reqmsg.getRequestType()] + " request");
				return;
			}

			// Log invocation
			String reqType = DataMessage.REQ_TYPES[reqmsg.getRequestType()];
			log.info("Data Request (" + reqType + ") from " + env.getOwnerID());
			ctx.setMessage("Processing Data Request (" + reqType + ") from " + env.getOwnerID());
		} else {
			cmd = _commands.get(new Integer(msg.getType()));
			if (cmd == null) {
				log.warn("No command for " + Message.MSG_TYPES[msg.getType()] + " message");
				return;
			}
		}

		// Execute the command
		cmd.execute(ctx, env);

		// Calculate and log execution time
		long execTime = System.currentTimeMillis() - startTime;
		CommandStats.log(cmd.getClass(), execTime);
		if (execTime > cmd.getMaxExecTime())
			log.warn(cmd.getClass().getName() + " completed in " + execTime + "ms");
	}

	/**
	 * Executes the Thread.
	 */
	public void run() {
		log.info("Started");
		_status.setStatus(WorkerStatus.STATUS_START);

		// Keep running until we're interrupted
		while (!Thread.currentThread().isInterrupted()) {
			long startTime = System.currentTimeMillis();
			while (MessageStack.MSG_INPUT.hasNext()) {
				MessageEnvelope env = MessageStack.MSG_INPUT.pop();
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
					MessageStack.MSG_OUTPUT.wakeup(false);
					startTime = System.currentTimeMillis();
				}
			}

			// Notify everyone waiting on the output stack
			MessageStack.MSG_OUTPUT.wakeup(false);

			// Wait on the input queue
			_status.setMessage("Idle");
			MessageStack.MSG_INPUT.waitForActivity();
		}
	}
}