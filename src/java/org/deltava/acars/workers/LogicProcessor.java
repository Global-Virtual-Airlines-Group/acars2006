// Copyright 2004, 2005, 2006, 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.*;
import java.sql.Connection;
import java.util.concurrent.locks.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.command.data.*;
import org.deltava.acars.message.*;

import org.deltava.dao.acars.*;

import org.deltava.jdbc.ConnectionPool;
import org.deltava.util.system.SystemData;

/**
 * An ACARS Worker thread to process messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class LogicProcessor extends Worker {
	
	private static final ReentrantReadWriteLock _flushLock = new ReentrantReadWriteLock(true);
	private static final Lock w = _flushLock.writeLock();

	private final Map<Integer, ACARSCommand> _commands = new HashMap<Integer, ACARSCommand>();
	private final Map<Integer, DataCommand> _dataCommands = new HashMap<Integer, DataCommand>();
	
	private final LatencyTracker _latency = new LatencyTracker(128);

	/**
	 * Initializes the Worker.
	 * @param threadID the worker instance ID
	 */
	public LogicProcessor(int threadID) {
		super("Message Processor-" + String.valueOf(threadID), LogicProcessor.class.getName() + "-" + threadID);
	}

	/**
	 * Initializes the Command map.
	 * @see Worker#open()
	 */
	public synchronized void open() {
		super.open();

		// Initialize commands
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

		// Initialize data commands
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
			log.error(Message.MSG_TYPES[msg.getType()] + " Security Exception from " + env.getOwnerID());
			return;
		}
		
		// If the message has high latency, warn
		long msgLatency = startTime - env.getTime();
		_latency.add(msgLatency);
		if (msgLatency > 500)
			log.warn(Message.MSG_TYPES[msg.getType()] + " from " + env.getOwnerID() + " has " + msgLatency + "ms latency");

		// Initialize the command context
		CommandContext ctx = new CommandContext(_pool, env, _status);

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
		SetStatistics.queue(new CommandEntry(cmd.getClass(), execTime));
		if (execTime > cmd.getMaxExecTime())
			log.warn(cmd.getClass().getName() + " completed in " + execTime + "ms");
		
		// If it's been too long since our last command purge, then purge
		if (w.tryLock()) {
			if (SetStatistics.getMaxAge() > 30000) {
				try {
					Connection con = ctx.getConnection(true);
					SetStatistics dao = new SetStatistics(con);
					int entries = dao.flush();
					if (log.isDebugEnabled())
						log.info("Flushed " + entries + " cached statistics entries");
				} catch (Exception e) {
					log.error("Error flushing positions - " + e.getMessage(), e);
				} finally {
					ctx.release();
				}
			}
			
			// Release the lock
			while (_flushLock.isWriteLockedByCurrentThread())
				w.unlock();
		}
	}
	
	/**
	 * Shuts down the worker.
	 */
	public final void close() {
		Connection con = null;
		ConnectionPool cp = (ConnectionPool) SystemData.getObject(SystemData.JDBC_POOL);
		try {
			con = cp.getConnection(true);
			synchronized (SetPosition.class) {
				if (SetPosition.size() > 0) {
					SetPosition dao = new SetPosition(con);
					int entries = dao.flush();
					log.warn("Flushed " + entries + " cached Position entries");
				}
			}

			// Flush statis
			SetStatistics dao = new SetStatistics(con);
			dao.flush();
		} catch (Exception e) {
			log.error("Error flushing Position/Statistics caches - " + e.getMessage(), e);
		} finally {
			cp.release(con);
		}
		
		super.close();
	}

	/**
	 * Executes the Thread.
	 */
	public void run() {
		log.info("Started");
		_status.setStatus(WorkerStatus.STATUS_START);

		// Keep running until we're interrupted
		while (!Thread.currentThread().isInterrupted()) {
			try {
				MessageEnvelope env = MSG_INPUT.take();
				_status.execute();
				process(env);
				_status.complete();
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				log.error("Error Processing Message - " + e.getMessage(), e);
			} finally {
				_status.setMessage("Idle - average latency " + _latency.getLatency() + "ms");	
			}
		}
	}
}