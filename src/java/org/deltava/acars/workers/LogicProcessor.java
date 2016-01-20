// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.*;
import java.util.concurrent.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.command.data.*;
import org.deltava.acars.command.dispatch.*;
import org.deltava.acars.command.mp.*;
import org.deltava.acars.message.*;
import org.deltava.acars.pool.*;

import org.gvagroup.ipc.WorkerStatus;

import org.deltava.util.system.SystemData;

/**
 * An ACARS Worker thread to process messages.
 * @author Luke
 * @version 6.4
 * @since 1.0
 */

public class LogicProcessor extends Worker {

	private QueueingThreadPool _cmdPool;

	private final Map<Integer, ACARSCommand> _commands = new HashMap<Integer, ACARSCommand>();
	private final Map<Integer, DataCommand> _dataCommands = new HashMap<Integer, DataCommand>();
	private final Map<Integer, DispatchCommand> _dspCommands = new HashMap<Integer, DispatchCommand>();

	/**
	 * Initializes the Worker.
	 */
	public LogicProcessor() {
		super("Message Processor", 40, LogicProcessor.class);
	}

	/**
	 * Initializes the Command map.
	 */
	@Override
	public void open() {
		super.open();
		int minThreads = Math.max(1, SystemData.getInt("acars.pool.threads.logic.min", 1));
		int maxThreads = Math.max(minThreads, SystemData.getInt("acars.pool.threads.logic.max", minThreads));
		_cmdPool = new QueueingThreadPool(minThreads, maxThreads, 1250, LogicProcessor.class);
		_cmdPool.allowCoreThreadTimeOut(false);
		_cmdPool.prestartCoreThread();
		_cmdPool.setSortBase(_status.getSortOrder());

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
		_commands.put(Integer.valueOf(Message.MSG_TOTD), new TakeoffCommand());
		_commands.put(Integer.valueOf(Message.MSG_MPUPDATE), new MPInfoCommand());
		_commands.put(Integer.valueOf(Message.MSG_MPINIT), new InitCommand());
		_commands.put(Integer.valueOf(Message.MSG_MPREMOVE), new RemoveCommand());
		_commands.put(Integer.valueOf(Message.MSG_VOICETOGGLE), new VoiceToggleCommand());
		_commands.put(Integer.valueOf(Message.MSG_VOICE), new VoiceMixCommand());
		_commands.put(Integer.valueOf(Message.MSG_MUTE), new MuteCommand());
		_commands.put(Integer.valueOf(Message.MSG_SWCHAN), new SwitchChannelCommand());
		_commands.put(Integer.valueOf(Message.MSG_WARN), new WarnCommand());
		_commands.put(Integer.valueOf(Message.MSG_COMPRESS), new CompressionCommand());
		_commands.put(Integer.valueOf(Message.MSG_SYSINFO), new SystemInfoCommand());

		// Initialize data commands
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_BUSY), new BusyCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_HIDE), new HiddenCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_CHARTS), new ChartsCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_DRAFTPIREP), new DraftFlightCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_USRLIST), new UserListCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_VALIDATE), new FlightValidationCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_ALLIST), new AirlineListCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_APLIST), new AirportListCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_EQLIST), new EquipmentListCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_SCHED), new ScheduleInfoCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_NAVAIDINFO), new NavaidCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_TS2SERVERS), new TS2ServerListCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_PVTVOX), new PrivateVoiceCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_ATCINFO), new ATCInfoCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_NATS), new OceanicTrackCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_LIVERIES), new LiveryListCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_WX), new WeatherCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_APINFO), new AirportInfoCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_APPINFO), new AppInfoCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_CHLIST), new VoiceChannelListCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_LOAD), new LoadFactorCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_ONLINE), new OnlinePresenceCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_LASTAP), new LastAirportCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_ALT), new AlternateAirportCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_IATA), new IATACodeCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_FLIGHTNUM), new FlightNumberCommand());
		_dataCommands.put(Integer.valueOf(DataMessage.REQ_FIR), new FIRSearchCommand());

		// Initialize dispatch commands
		_dspCommands.put(Integer.valueOf(DispatchMessage.DSP_SVCREQ), new ServiceRequestCommand());
		_dspCommands.put(Integer.valueOf(DispatchMessage.DSP_CANCEL), new org.deltava.acars.command.dispatch.CancelCommand());
		_dspCommands.put(Integer.valueOf(DispatchMessage.DSP_ACCEPT), new org.deltava.acars.command.dispatch.AcceptCommand());
		_dspCommands.put(Integer.valueOf(DispatchMessage.DSP_INFO), new FlightDataCommand());
		_dspCommands.put(Integer.valueOf(DispatchMessage.DSP_ROUTEREQ), new RouteRequestCommand());
		_dspCommands.put(Integer.valueOf(DispatchMessage.DSP_COMPLETE), new ServiceCompleteCommand());
		_dspCommands.put(Integer.valueOf(DispatchMessage.DSP_PROGRESS), new ProgressCommand());
		_dspCommands.put(Integer.valueOf(DispatchMessage.DSP_RANGE), new ServiceRangeCommand());
		_dspCommands.put(Integer.valueOf(DispatchMessage.DSP_SCOPEINFO), new ScopeInfoCommand());
		_dspCommands.put(Integer.valueOf(DispatchMessage.DSP_ROUTEPLOT), new RoutePlotCommand());

		int size = _commands.size() + _dataCommands.size() + _dspCommands.size();
		log.info("Loaded " + size + " commands");
	}

	private class CommandWorker extends PoolWorker {

		private final MessageEnvelope _env;
		private final ACARSCommand _cmd;

		CommandWorker(MessageEnvelope env, ACARSCommand cmd) {
			super();
			_env = env;
			_cmd = cmd;
		}

		@Override
		public String getName() {
			return "CommandProcessor";
		}

		@Override
		public void run() {
			if ((_env == null) || (_cmd == null)) return;

			// Get the message and start time
			Message msg = _env.getMessage();
			if (msg.getType() == Message.MSG_DATAREQ) {
				DataMessage dmsg = (DataMessage) msg;
				_status.setMessage("Processing " + DataMessage.REQ_TYPES[dmsg.getRequestType()] + " message from " + _env.getOwnerID());
			} else
				_status.setMessage("Processing " + Message.MSG_TYPES[msg.getType()] + " message from " + _env.getOwnerID());

			// Check if we can be anonymous
			boolean isAuthenticated = (_env.getOwner() != null);
			if (isAuthenticated == msg.isAnonymous()) {
				String errorMsg = Message.MSG_TYPES[msg.getType()] + " Security Exception from " + _env.getOwnerID();
				ACARSConnection ac = _pool.get(_env.getConnectionID());
				if (ac != null)
					errorMsg += " (" + ac.getRemoteAddr() + ")";
				
				log.error(errorMsg);

				// Return an ACK requesting a login
				if (!isAuthenticated) {
					AcknowledgeMessage ackMsg = new AcknowledgeMessage(null, msg.getID());
					ackMsg.setEntry("auth", "true");
					ackMsg.setTime(msg.getTime());
					MessageEnvelope env = new MessageEnvelope(ackMsg, _env.getConnectionID());
					env.setCritical(true);
					MSG_OUTPUT.add(env);
				}

				return;
			}

			// If the message has high latency, warn
			long startTime = System.nanoTime();
			long msgLatency = startTime - _env.getTime();
			_status.add(msgLatency);
			msgLatency = TimeUnit.MILLISECONDS.convert(msgLatency, TimeUnit.NANOSECONDS);
			if (msgLatency > 500)
				log.warn(Message.MSG_TYPES[msg.getType()] + " from " + _env.getOwnerID() + " has " + msgLatency + "ms latency");

			// Initialize the command context and execute the command
			CommandContext ctx = new CommandContext(_pool, _env, _status);
			_cmd.execute(ctx, _env);

			// Calculate and log execution time
			long execTime = TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
			if (execTime > _cmd.getMaxExecTime()) log.warn(_cmd.getClass().getName() + " completed in " + execTime + "ms");
		}
	}

	/**
	 * Returns the status of this Worker and the Connection writers.
	 * @return a List of WorkerStatus beans, with this Worker's status first
	 */
	@Override
	public final List<WorkerStatus> getStatus() {
		List<WorkerStatus> results = new ArrayList<WorkerStatus>(super.getStatus());
		results.addAll(_cmdPool.getWorkerStatus());
		return results;
	}

	/**
	 * Shuts down the worker.
	 */
	@Override
	public final void close() {

		// Wait for the pool to shut down
		try {
			_cmdPool.shutdown();
			_cmdPool.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException ie) {
			// empty
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		super.close();
	}

	/**
	 * Executes the Thread.
	 */
	@Override
	public void run() {
		log.info("Started");
		_status.setStatus(WorkerStatus.STATUS_START);

		// Keep running until we're interrupted
		while (!Thread.currentThread().isInterrupted()) {
			_status.setMessage("Idle - " + _cmdPool.getPoolSize() + " threads");
			try {
				MessageEnvelope env = MSG_INPUT.poll(30, TimeUnit.SECONDS);
				_status.execute();

				// Log the received message and get the command to process it
				while (env != null) {
					_status.setMessage("Processing Message");
					Message msg = env.getMessage();
					if (log.isDebugEnabled())
						log.debug(Message.MSG_TYPES[msg.getType()] + " message from " + env.getOwnerID());

					ACARSCommand cmd = null;
					if (msg.getType() == Message.MSG_DATAREQ) {
						DataRequestMessage reqmsg = (DataRequestMessage) msg;
						cmd = _dataCommands.get(Integer.valueOf(reqmsg.getRequestType()));
						String reqType = DataMessage.REQ_TYPES[reqmsg.getRequestType()];
						if (cmd == null)
							log.warn("No Data Command for " + reqType + " request");
						else
							log.info("Data Request (" + reqType + ") from " + env.getOwnerID());
					} else if (msg.getType() == Message.MSG_DISPATCH) {
						DispatchMessage dspmsg = (DispatchMessage) msg;
						cmd = _dspCommands.get(Integer.valueOf(dspmsg.getRequestType()));
						String reqType = DispatchMessage.REQ_TYPES[dspmsg.getRequestType()];
						if (cmd == null)
							log.warn("No Dispatch Command for " + reqType + " request");
						else
							log.info("Dispatch Request (" + reqType + ") from " + env.getOwnerID());
					} else {
						cmd = _commands.get(Integer.valueOf(msg.getType()));
						if (cmd == null)
							log.warn("No command for " + Message.MSG_TYPES[msg.getType()] + " message");
					}

					// Send the envelope to the thread pool for processing
					if (cmd != null)
						_cmdPool.execute(new CommandWorker(env, cmd));

					env = MSG_INPUT.poll();
				}

				_status.complete();
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				log.error("Error Processing Message - " + e.getMessage(), e);
			}
		}
	}
}