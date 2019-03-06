// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.*;
import java.util.concurrent.*;

import com.newrelic.api.agent.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.command.data.*;
import org.deltava.acars.command.dispatch.*;
import org.deltava.acars.command.mp.*;
import org.deltava.acars.message.*;
import org.deltava.acars.pool.*;

import org.gvagroup.ipc.*;

import org.deltava.util.log.*;
import org.deltava.util.system.SystemData;

/**
 * An ACARS Worker thread to process messages.
 * @author Luke
 * @version 8.6
 * @since 1.0
 */

public class LogicProcessor extends Worker {

	private QueueingThreadPool _cmdPool;

	private final Map<MessageType, ACARSCommand> _commands = new HashMap<MessageType, ACARSCommand>();
	private final Map<DataRequest, DataCommand> _dataCommands = new HashMap<DataRequest, DataCommand>();
	private final Map<DispatchRequest, DispatchCommand> _dspCommands = new HashMap<DispatchRequest, DispatchCommand>();

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
		_commands.put(MessageType.ACK, new DummyCommand());
		_commands.put(MessageType.PING, new AcknowledgeCommand(true));
		_commands.put(MessageType.POSITION, new PositionCommand());
		_commands.put(MessageType.TEXT, new TextMessageCommand());
		_commands.put(MessageType.AUTH, new AuthenticateCommand());
		_commands.put(MessageType.INFO, new InfoCommand());
		_commands.put(MessageType.ENDFLIGHT, new EndFlightCommand());
		_commands.put(MessageType.QUIT, new QuitCommand());
		_commands.put(MessageType.PIREP, new FilePIREPCommand());
		_commands.put(MessageType.ERROR, new ErrorCommand());
		_commands.put(MessageType.DIAG, new DiagnosticCommand());
		_commands.put(MessageType.TOTD, new TakeoffCommand());
		_commands.put(MessageType.MPUPDATE, new MPInfoCommand());
		_commands.put(MessageType.MPINIT, new InitCommand());
		_commands.put(MessageType.MPREMOVE, new RemoveCommand());
		_commands.put(MessageType.VOICETOGGLE, new VoiceToggleCommand());
		_commands.put(MessageType.VOICE, new VoiceMixCommand());
		_commands.put(MessageType.MUTE, new MuteCommand());
		_commands.put(MessageType.SWCHAN, new SwitchChannelCommand());
		_commands.put(MessageType.WARN, new WarnCommand());
		_commands.put(MessageType.COMPRESS, new CompressionCommand());
		_commands.put(MessageType.SYSINFO, new SystemInfoCommand());

		// Initialize data commands
		_dataCommands.put(DataRequest.BUSY, new BusyCommand());
		_dataCommands.put(DataRequest.HIDE, new HiddenCommand());
		_dataCommands.put(DataRequest.CHARTS, new ChartsCommand());
		_dataCommands.put(DataRequest.DRAFTPIREP, new DraftFlightCommand());
		_dataCommands.put(DataRequest.USERLIST, new UserListCommand());
		_dataCommands.put(DataRequest.VALIDATE, new FlightValidationCommand());
		_dataCommands.put(DataRequest.ALLIST, new AirlineListCommand());
		_dataCommands.put(DataRequest.APLIST, new AirportListCommand());
		_dataCommands.put(DataRequest.EQLIST, new EquipmentListCommand());
		_dataCommands.put(DataRequest.SCHED, new ScheduleInfoCommand());
		_dataCommands.put(DataRequest.NAVAIDINFO, new NavaidCommand());
		_dataCommands.put(DataRequest.TS2SERVERS, new TS2ServerListCommand());
		_dataCommands.put(DataRequest.PVTVOX, new PrivateVoiceCommand());
		_dataCommands.put(DataRequest.ATCINFO, new ATCInfoCommand());
		_dataCommands.put(DataRequest.NATS, new OceanicTrackCommand());
		_dataCommands.put(DataRequest.LIVERIES, new LiveryListCommand());
		_dataCommands.put(DataRequest.WX, new WeatherCommand());
		_dataCommands.put(DataRequest.APINFO, new AirportInfoCommand());
		_dataCommands.put(DataRequest.APPINFO, new AppInfoCommand());
		_dataCommands.put(DataRequest.CHLIST, new VoiceChannelListCommand());
		_dataCommands.put(DataRequest.LOAD, new LoadFactorCommand());
		_dataCommands.put(DataRequest.ONLINE, new OnlinePresenceCommand());
		_dataCommands.put(DataRequest.LASTAP, new LastAirportCommand());
		_dataCommands.put(DataRequest.ALT, new AlternateAirportCommand());
		_dataCommands.put(DataRequest.IATA, new IATACodeCommand());
		_dataCommands.put(DataRequest.FLIGHTNUM, new FlightNumberCommand());
		_dataCommands.put(DataRequest.FIR, new FIRSearchCommand());
		_dataCommands.put(DataRequest.RUNWAYS, new PopularRunwaysCommand());
		_dataCommands.put(DataRequest.GATES, new GateListCommand());
		_dataCommands.put(DataRequest.RWYINFO, new RunwayInfoCommand());

		// Initialize dispatch commands
		_dspCommands.put(DispatchRequest.SVCREQ, new ServiceRequestCommand());
		_dspCommands.put(DispatchRequest.CANCEL, new org.deltava.acars.command.dispatch.CancelCommand());
		_dspCommands.put(DispatchRequest.ACCEPT, new org.deltava.acars.command.dispatch.AcceptCommand());
		_dspCommands.put(DispatchRequest.INFO, new FlightDataCommand());
		_dspCommands.put(DispatchRequest.ROUTEREQ, new RouteRequestCommand());
		_dspCommands.put(DispatchRequest.COMPLETE, new ServiceCompleteCommand());
		_dspCommands.put(DispatchRequest.PROGRESS, new ProgressCommand());
		_dspCommands.put(DispatchRequest.RANGE, new ServiceRangeCommand());
		_dspCommands.put(DispatchRequest.SCOPEINFO, new ScopeInfoCommand());
		_dspCommands.put(DispatchRequest.ROUTEPLOT, new RoutePlotCommand());

		int size = _commands.size() + _dataCommands.size() + _dspCommands.size();
		log.info("Loaded " + size + " commands");
	}

	private class CommandWorker extends PoolWorker {

		private final MessageEnvelope _env;
		private final ACARSCommand _cmd;
		private final String _reqType;

		CommandWorker(MessageEnvelope env, ACARSCommand cmd, String reqType) {
			super();
			_env = env;
			_cmd = cmd;
			_reqType = reqType;
		}

		@Override
		public String getName() {
			return "CommandProcessor";
		}

		@Override
		@Trace(dispatcher=true)
		public void run() {
			if ((_env == null) || (_cmd == null)) return;

			// Get the message and start time
			Message msg = _env.getMessage();
			_status.setMessage("Processing " + _reqType + " message from " + _env.getOwnerID());

			// Check if we can be anonymous
			boolean isAuthenticated = (_env.getOwner() != null);
			if (isAuthenticated == msg.isAnonymous()) {
				String errorMsg = _reqType + " Security Exception from " + _env.getOwnerID();
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
				log.warn(_reqType + " from " + _env.getOwnerID() + " has " + msgLatency + "ms latency");

			// Initialize the command context and execute the command
			CommandContext ctx = new CommandContext(_pool, _env, _status);
			_cmd.execute(ctx, _env);

			// Calculate and log execution time
			long execTime = TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
			NewRelic.recordResponseTimeMetric(_cmd.getClass().getSimpleName(), execTime);
			if (execTime > _cmd.getMaxExecTime()) log.warn(_cmd.getClass().getName() + " completed in " + execTime + "ms");

			// Instrumentation
			NewRelic.setRequestAndResponse(new SyntheticRequest(_reqType, _env.getOwnerID()), new SyntheticResponse());
			NewRelic.setTransactionName("ACARS", _cmd.getClass().getSimpleName());
			if (!msg.isAnonymous())
				NewRelic.setUserName(msg.getSender().getName());
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
		_status.setStatus(WorkerState.RUNNING);

		// Keep running until we're interrupted
		while (!Thread.currentThread().isInterrupted()) {
			_status.setMessage("Idle - " + _cmdPool.getPoolSize() + " threads");
			try {
				MessageEnvelope env = MSG_INPUT.poll(30, TimeUnit.SECONDS);
				_status.execute();

				// Log the received message and get the command to process it
				while (env != null) {
					_status.setMessage("Processing Message");
					Message msg = env.getMessage(); String reqType = msg.getType().getType();
					if (log.isDebugEnabled())
						log.debug(reqType + " message from " + env.getOwnerID());

					ACARSCommand cmd = null;
					if (msg.getType() == MessageType.DATAREQ) {
						DataRequestMessage reqmsg = (DataRequestMessage) msg;
						cmd = _dataCommands.get(reqmsg.getRequestType());
						reqType = reqmsg.getRequestType().getType();
						if (cmd == null)
							log.warn("No Data Command for " + reqType + " request");
						else
							log.info("Data Request (" + reqType + ") from " + env.getOwnerID());
					} else if (msg.getType() == MessageType.DISPATCH) {
						DispatchMessage dspmsg = (DispatchMessage) msg;
						cmd = _dspCommands.get(dspmsg.getRequestType());
						reqType = dspmsg.getRequestType().getCode();
						if (cmd == null)
							log.warn("No Dispatch Command for " + reqType + " request");
						else
							log.info("Dispatch Request (" + reqType + ") from " + env.getOwnerID());
					} else {
						cmd = _commands.get(msg.getType());
						if (cmd == null)
							log.warn("No command for " + reqType + " message");
					}

					// Send the envelope to the thread pool for processing
					if (cmd != null) _cmdPool.execute(new CommandWorker(env, cmd, reqType));
					env = MSG_INPUT.poll();
				}
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				log.error("Error Processing Message - " + e.getMessage(), e);
			} finally {
				_status.complete();				
			}
		}
	}
}