// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020, 2022 Global Virtual Airlines Group. All Rights Reserved.
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

import org.deltava.beans.acars.CommandStats;
import org.deltava.beans.system.APILogger;
import org.gvagroup.common.SharedData;
import org.gvagroup.ipc.*;

import org.deltava.util.log.*;
import org.deltava.util.system.SystemData;

/**
 * An ACARS Worker thread to process messages.
 * @author Luke
 * @version 10.3
 * @since 1.0
 */

public class LogicProcessor extends Worker {

	private QueueingThreadPool _cmdPool;
	
	private final HashMap<String, CommandStats> _cmdStats = new HashMap<String, CommandStats>();

	private final Map<MessageType, ACARSCommand> _commands = new HashMap<MessageType, ACARSCommand>();
	private final Map<SubRequest, ACARSCommand> _subCommands = new HashMap<SubRequest, ACARSCommand>();

	/**
	 * Initializes the Worker.
	 */
	public LogicProcessor() {
		super("Message Processor", 40, LogicProcessor.class);
	}

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
		_commands.put(MessageType.PING, new PingCommand());
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
		_commands.put(MessageType.WARN, new WarnCommand());
		_commands.put(MessageType.COMPRESS, new CompressionCommand());
		_commands.put(MessageType.SYSINFO, new SystemInfoCommand());
		_commands.put(MessageType.PERFORMANCE, new PerformanceCommand());
		_commands.put(MessageType.DISCONNECT, new KickCommand());
		_commands.forEach((id, cmd) -> _cmdStats.put(cmd.getClass().getName(), new CommandStats(cmd.getClass().getSimpleName())));

		// Initialize data commands
		_subCommands.put(DataRequest.BUSY, new BusyCommand());
		_subCommands.put(DataRequest.HIDE, new HiddenCommand());
		_subCommands.put(DataRequest.CHARTS, new ChartsCommand());
		_subCommands.put(DataRequest.DRAFTPIREP, new DraftFlightCommand());
		_subCommands.put(DataRequest.USERLIST, new UserListCommand());
		_subCommands.put(DataRequest.VALIDATE, new FlightValidationCommand());
		_subCommands.put(DataRequest.ALLIST, new AirlineListCommand());
		_subCommands.put(DataRequest.APLIST, new AirportListCommand());
		_subCommands.put(DataRequest.EQLIST, new EquipmentListCommand());
		_subCommands.put(DataRequest.SCHED, new ScheduleInfoCommand());
		_subCommands.put(DataRequest.NAVAIDINFO, new NavaidCommand());
		_subCommands.put(DataRequest.PVTVOX, new PrivateVoiceCommand());
		_subCommands.put(DataRequest.ATCINFO, new ATCInfoCommand());
		_subCommands.put(DataRequest.NATS, new OceanicTrackCommand());
		_subCommands.put(DataRequest.LIVERIES, new LiveryListCommand());
		_subCommands.put(DataRequest.WX, new WeatherCommand());
		_subCommands.put(DataRequest.APINFO, new AirportInfoCommand());
		_subCommands.put(DataRequest.APPINFO, new AppInfoCommand());
		_subCommands.put(DataRequest.LOAD, new LoadFactorCommand());
		_subCommands.put(DataRequest.ONLINE, new OnlinePresenceCommand());
		_subCommands.put(DataRequest.LASTAP, new LastAirportCommand());
		_subCommands.put(DataRequest.ALT, new AlternateAirportCommand());
		_subCommands.put(DataRequest.IATA, new IATACodeCommand());
		_subCommands.put(DataRequest.FLIGHTNUM, new FlightNumberCommand());
		_subCommands.put(DataRequest.FIR, new FIRSearchCommand());
		_subCommands.put(DataRequest.RUNWAYS, new PopularRunwaysCommand());
		_subCommands.put(DataRequest.GATES, new GateListCommand());
		_subCommands.put(DataRequest.RWYINFO, new RunwayInfoCommand());
		_subCommands.put(DataRequest.TAXITIME, new TaxiTimeCommand());
		_subCommands.put(DataRequest.SBDATA, new SimBriefTextCommand());
		_subCommands.put(DataRequest.ATIS, new ATISCommand());

		// Initialize dispatch commands
		_subCommands.put(DispatchRequest.SVCREQ, new ServiceRequestCommand());
		_subCommands.put(DispatchRequest.CANCEL, new org.deltava.acars.command.dispatch.CancelCommand());
		_subCommands.put(DispatchRequest.ACCEPT, new org.deltava.acars.command.dispatch.AcceptCommand());
		_subCommands.put(DispatchRequest.INFO, new FlightDataCommand());
		_subCommands.put(DispatchRequest.ROUTEREQ, new RouteRequestCommand());
		_subCommands.put(DispatchRequest.COMPLETE, new ServiceCompleteCommand());
		_subCommands.put(DispatchRequest.PROGRESS, new ProgressCommand());
		_subCommands.put(DispatchRequest.RANGE, new ServiceRangeCommand());
		_subCommands.put(DispatchRequest.SCOPEINFO, new ScopeInfoCommand());
		_subCommands.put(DispatchRequest.ROUTEPLOT, new RoutePlotCommand());
		_subCommands.forEach((id, cmd) -> _cmdStats.put(cmd.getClass().getName(), new CommandStats(cmd.getClass().getSimpleName())));

		int size = _commands.size() + _subCommands.size();
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
			
			// Get stats entry
			CommandStats stats = _cmdStats.get(_cmd.getClass().getName());
			stats.increment();

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
			
			// TODO: Eventually log these to the correct airline
			APILogger.drain();

			// Calculate and log execution time
			long execTime = TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
			stats.success(execTime, ctx.getBackEndTime());
			NewRelic.recordResponseTimeMetric(_cmd.getClass().getSimpleName(), execTime);
			if (execTime > _cmd.getMaxExecTime()) log.warn(_cmd.getClass().getName() + " completed in " + execTime + "ms");

			// Instrumentation
			NewRelic.setRequestAndResponse(new SyntheticRequest(_reqType, _env.getOwnerID()), new SyntheticResponse());
			NewRelic.setTransactionName("ACARS", _cmd.getClass().getSimpleName());
			if (!msg.isAnonymous())
				NewRelic.setUserName(msg.getSender().getName());
		}
	}

	@Override
	public final List<WorkerStatus> getStatus() {
		List<WorkerStatus> results = new ArrayList<WorkerStatus>(super.getStatus());
		results.addAll(_cmdPool.getWorkerStatus());
		return results;
	}

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

	@Override
	public void run() {
		log.info("Started");
		_status.setStatus(WorkerState.RUNNING);

		// Keep running until we're interrupted
		while (!Thread.currentThread().isInterrupted()) {
			_status.setMessage("Idle - " + _cmdPool.getPoolSize() + " threads");
			try {
				SharedData.addData(SharedData.ACARS_CMDSTATS, _cmdStats);
				MessageEnvelope env = MSG_INPUT.poll(30, TimeUnit.SECONDS);
				_status.execute();

				// Log the received message and get the command to process it
				while (env != null) {
					_status.setMessage("Processing Message");
					Message msg = env.getMessage(); String reqType = msg.getType().getDescription();
					if (log.isDebugEnabled())
						log.debug(reqType + " message from " + env.getOwnerID());

					ACARSCommand cmd = null;
					if (msg instanceof SubRequestMessage) {
						SubRequestMessage srmsg = (SubRequestMessage) msg;
						cmd = _subCommands.get(srmsg.getRequestType());
						reqType = srmsg.getRequestType().getCode();
						if (cmd != null) {
							log.info("Data Request (" + reqType + ") from " + env.getOwnerID());
							_cmdPool.execute(new CommandWorker(env, cmd, reqType));
						} else
							log.warn("No " + srmsg.getRequestType().getType() + " Command for " + reqType + " request");
					} else {
						cmd = _commands.get(msg.getType());
						if (cmd != null)
							_cmdPool.execute(new CommandWorker(env, cmd, reqType));
						else
							log.warn("No command for " + reqType + " message");
					}

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