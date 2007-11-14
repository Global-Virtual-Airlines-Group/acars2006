// Copyright 2004, 2005, 2006, 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.sql.Connection;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.command.data.*;
import org.deltava.acars.message.*;
import org.deltava.acars.pool.*;

import org.deltava.dao.acars.*;

import org.deltava.jdbc.ConnectionPool;
import org.deltava.util.system.SystemData;

/**
 * An ACARS Worker thread to process messages.
 * @author Luke
 * @version 2.0
 * @since 1.0
 */

public class LogicProcessor extends Worker {
	
	private QueueingThreadPool _cmdPool;
	
	private final Map<Integer, ACARSCommand> _commands = new HashMap<Integer, ACARSCommand>();
	private final Map<Integer, DataCommand> _dataCommands = new HashMap<Integer, DataCommand>();
	
	/**
	 * Initializes the Worker.
	 */
	public LogicProcessor() {
		super("Message Processor", LogicProcessor.class);
	}

	/**
	 * Initializes the Command map.
	 */
	public void open() {
		super.open();
		int minThreads = Math.max(1, SystemData.getInt("acars.pool.threads.min", 1));
		int maxThreads = SystemData.getInt("acars.pool.threads.logic.max", minThreads);
		_cmdPool = new QueueingThreadPool(minThreads, maxThreads, 2000, LogicProcessor.class);

		// Initialize commands
		_commands.put(Integer.valueOf(Message.MSG_ACK), new DummyCommand());
		_commands.put(Integer.valueOf(Message.MSG_PING), new AcknowledgeCommand(true));
		_commands.put(Integer.valueOf(Message.MSG_POSITION), new PositionCommand());
		_commands.put(Integer.valueOf(Message.MSG_TEXT), new TextMessageCommand());
		_commands.put(Integer.valueOf(Message.MSG_AUTH), new AuthenticateCommand());
		_commands.put(Integer.valueOf(Message.MSG_INFO), new InfoCommand());
		_commands.put(Integer.valueOf(Message.MSG_ENDFLIGHT), new EndFlightCommand());
		_commands.put(Integer.valueOf(Message.MSG_QUIT), new QuitCommand());
		_commands.put(Integer.valueOf(Message.MSG_PIREP), new FilePIREPCommand());
		_commands.put(Integer.valueOf(Message.MSG_ERROR), new ErrorCommand());
		_commands.put(Integer.valueOf(Message.MSG_DIAG), new DiagnosticCommand());
		_commands.put(Integer.valueOf(Message.MSG_DISPATCH), new DispatchCommand());

		// Initialize data commands
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_BUSY), new BusyCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_CHARTS), new ChartsCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_DRAFTPIREP), new DraftFlightCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_USRLIST), new UserListCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_ALLIST), new AirlineListCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_APLIST), new AirportListCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_EQLIST), new EquipmentListCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_SCHED), new ScheduleInfoCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_NAVAIDINFO), new NavaidCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_TS2SERVERS), new TS2ServerListCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_PVTVOX), new PrivateVoiceCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_ATCINFO), new ATCInfoCommand());
		log.info("Loaded " + (_commands.size() + _dataCommands.size()) + " commands");
	}
	
	private class CommandWorker extends PoolWorker {
		
		private MessageEnvelope _env;
		private ACARSCommand _cmd;
		
		CommandWorker(MessageEnvelope env, ACARSCommand cmd) {
			super();
			_env = env;
			_cmd = cmd;
		}
		
		public String getName() {
			return "CommandProcessor";
		}
		
		public void run() {
			if ((_env == null) || (_cmd == null))
				return;
			
			// Get the message and start time
			long startTime = System.currentTimeMillis();
			Message msg = _env.getMessage();
			_status.setMessage("Processing " + Message.MSG_TYPES[msg.getType()] + " message from " + _env.getOwnerID());

			// Check if we can be anonymous
			boolean isAuthenticated = (_env.getOwner() != null);
			if (isAuthenticated == msg.isAnonymous()) {
				log.error(Message.MSG_TYPES[msg.getType()] + " Security Exception from " + _env.getOwnerID());
				return;
			}
			
			// If the message has high latency, warn
			long msgLatency = startTime - _env.getTime();
			_status.add(msgLatency);
			if (msgLatency > 500)
				log.warn(Message.MSG_TYPES[msg.getType()] + " from " + _env.getOwnerID() + " has " + msgLatency + "ms latency");

			// Initialize the command context and execute the command
			CommandContext ctx = new CommandContext(_pool, _env, _status);
			_cmd.execute(ctx, _env);
			
			// Calculate and log execution time
			long execTime = System.currentTimeMillis() - startTime;
			int userID = (_env.getOwner() == null) ? 0 : _env.getOwner().getID();
			SetStatistics.queue(new CommandEntry(_cmd.getClass(), userID, execTime));
			if (execTime > _cmd.getMaxExecTime())
				log.warn(_cmd.getClass().getName() + " completed in " + execTime + "ms");
		}
	}

	private void flushLogs() {
		_status.setMessage("Flushing Command logs");
		Connection con = null;
		ConnectionPool cp = (ConnectionPool) SystemData.getObject(SystemData.JDBC_POOL);
		try {
			con = cp.getConnection(true);
			if (SetPosition.size() > 0) {
				SetPosition dao = new SetPosition(con);
				int entries = dao.flush();
				log.warn("Flushed " + entries + " cached Position entries");
			}

			// Flush statis
			SetStatistics dao = new SetStatistics(con);
			dao.flush();
		} catch (Exception e) {
			log.error("Error flushing Position/Statistics caches - " + e.getMessage(), e);
		} finally {
			cp.release(con);
		}
	}
	
	/**
	 * Returns the status of this Worker and the Connection writers.
	 * @return a List of WorkerStatus beans, with this Worker's status first
	 */
	public final List<WorkerStatus> getStatus() {
		List<WorkerStatus> results = new ArrayList<WorkerStatus>(super.getStatus());
		results.addAll(_cmdPool.getWorkerStatus());
		return results;
	}
	
	/**
	 * Shuts down the worker.
	 */
	public final void close() {
		
		// Wait for the pool to shut down
		try {
			_cmdPool.shutdown();
			_cmdPool.awaitTermination(1250, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			super.close();
		}
		
		flushLogs();
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
				
				// Log the received message and get the command to process it
				Message msg = env.getMessage();
				log.debug(Message.MSG_TYPES[msg.getType()] + " message from " + env.getOwnerID());
				ACARSCommand cmd = null;
				if (msg.getType() == Message.MSG_DATAREQ) {
					DataRequestMessage reqmsg = (DataRequestMessage) msg;
					cmd = _dataCommands.get(Integer.valueOf(reqmsg.getRequestType()));
					if (cmd == null) {
						log.warn("No Data Command for " + DataMessage.REQ_TYPES[reqmsg.getRequestType()] + " request");
						return;
					}

					// Log invocation
					String reqType = DataMessage.REQ_TYPES[reqmsg.getRequestType()];
					log.info("Data Request (" + reqType + ") from " + env.getOwnerID());
				} else {
					cmd = _commands.get(Integer.valueOf(msg.getType()));
					if (cmd == null) {
						log.warn("No command for " + Message.MSG_TYPES[msg.getType()] + " message");
						return;
					}
				}
				
				// Send the envelope to the thread pool for processing
				_cmdPool.execute(new CommandWorker(env, cmd));
				
				// Flush the logs
				if (SetStatistics.getMaxAge() > 30000) 
					flushLogs();
				
				_status.complete();
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				log.error("Error Processing Message - " + e.getMessage(), e);
			} finally {
				_status.setMessage("Idle");	
			}
		}
	}
}