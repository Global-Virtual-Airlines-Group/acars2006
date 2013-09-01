// Copyright 2007, 2008, 2009, 2010, 2011, 2013 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars;

import java.io.*;
import java.sql.*;
import java.util.*;

import javax.servlet.*;

import org.apache.log4j.*;

import org.deltava.dao.*;
import org.deltava.mail.MailerDaemon;

import org.deltava.acars.beans.*;

import org.deltava.acars.ipc.IPCDaemon;
import org.deltava.beans.flight.ETOPSHelper;
import org.deltava.beans.mvs.Channel;
import org.deltava.beans.schedule.Airport;

import org.deltava.security.Authenticator;

import org.deltava.util.*;
import org.deltava.util.cache.CacheLoader;
import org.deltava.util.system.SystemData;

import org.gvagroup.common.SharedData;
import org.gvagroup.jdbc.*;

/**
 * A servlet context listener to spawn ACARS in its own J2EE web application.
 * @author Luke
 * @version 5.1
 * @since 1.0
 */

public class SystemBootstrap implements ServletContextListener, Thread.UncaughtExceptionHandler {
	
	private final Logger log;
	
	private ConnectionPool _jdbcPool;
	
	private final ThreadGroup _dGroup = new ThreadGroup("ACARS System Daemons");
	private final Map<Thread, Runnable> _daemons = new HashMap<Thread, Runnable>();

	/**
	 * Initialize the System bootstrap loader, and configure log4j.
	 */
	public SystemBootstrap() {
		super();
		PropertyConfigurator.configure(getClass().getResource("/etc/log4j.properties"));
		log = Logger.getLogger(SystemBootstrap.class);
		log.info("Initialized log4j");
	}
	
	/**
	 * Web application termination callback handler.
	 */
	@Override
	public void contextDestroyed(ServletContextEvent e) {
		_dGroup.interrupt();
		_daemons.clear();
		
		// If ACARS is enabled, then clean out the active flags
		if (SystemData.getBoolean("airline.voice.ts2.enabled")) {
			log.info("Resetting TeamSpeak 2 client activity flags");

			Connection c = null;
			try {
				c = _jdbcPool.getConnection();
				SetTS2Data ts2wdao = new SetTS2Data(c);
				ts2wdao.clearActiveFlags();
			} catch (Exception de) {
				log.error(de.getMessage(), de);
			} finally {
				_jdbcPool.release(c);
			}
		}

		// Shut down the JDBC connection pool
		ThreadUtils.kill(_dGroup, 2500);
		_jdbcPool.close();
		JDBCUtils.cleanMySQLTimer();
		JDBCUtils.deregisterDrivers();
		java.beans.Introspector.flushCaches();

		// Close the Log4J manager
		SharedData.purge(SystemData.get("airline.code"));
		log.error("Shut down " + SystemData.get("airline.code"));
		ThreadUtils.sleep(200);
		LogManager.shutdown();
	}
	
	/**
	 * Web application initializaton callback handler.
	 */
	@Override
	public void contextInitialized(ServletContextEvent e) {
		e.getServletContext().setAttribute("startedOn", new java.util.Date());
		
		// Initialize system data
		SystemData.init();
		SharedData.addApp(SystemData.get("airline.code"));
		
		// Initialize the connection pool
		log.info("Starting JDBC connection pool");
		_jdbcPool = new ConnectionPool(SystemData.getInt("jdbc.pool_max_size", 2), SystemData.get("airline.code"));
		_jdbcPool.setProperties((Map<?, ?>) SystemData.getObject("jdbc.connectProperties"));
		_jdbcPool.setCredentials(SystemData.get("jdbc.user"), SystemData.get("jdbc.pwd"));
		_jdbcPool.setURL(SystemData.get("jdbc.url"));
		_jdbcPool.setMaxRequests(SystemData.getInt("jdbc.max_reqs", 0));
		_jdbcPool.setLogStack(SystemData.getBoolean("jdbc.log_stack"));
		
		// Attempt to load the driver and connect
		try {
			_jdbcPool.setDriver(SystemData.get("jdbc.driver"));
			_jdbcPool.connect(SystemData.getInt("jdbc.pool_size"));
		} catch (ClassNotFoundException cnfe) {
			log.error("Cannot load JDBC driver class - " + SystemData.get("jdbc.Driver"));
		} catch (ConnectionPoolException cpe) {
			log.error("Error connecting to JDBC data source - " + cpe.getCause().getMessage(), cpe.getCause());
		}
		
		// Save the connection pool in the SystemData
		SystemData.add(SystemData.JDBC_POOL, _jdbcPool);
		SharedData.addData(SharedData.JDBC_POOL + SystemData.get("airline.code"), _jdbcPool);
		
		// Initialize caches
		try (InputStream is = ConfigLoader.getStream("/etc/cacheInfo.xml")) {
			CacheLoader.load(is);
		} catch(IOException ie) {
			log.warn("Cannot configure caches from code");
		}
		
		// Get and load the authenticator
		String authClass = SystemData.get("security.auth");
		try {
			Class<?> c = Class.forName(authClass);
			log.debug("Loaded class " + authClass);
			Authenticator auth = (Authenticator) c.newInstance();

			// Initialize and store in the servlet context
			auth.init(Authenticator.DEFAULT_PROPS_FILE);
			SystemData.add(SystemData.AUTHENTICATOR, auth);
		} catch (ClassNotFoundException cnfe) {
			log.error("Cannot find authenticator class " + authClass);
		} catch (SecurityException se) {
			log.error("Error initializing authenticator - " + se.getMessage());
		} catch (Exception ex) {
			log.error("Error starting authenticator - " + ex.getClass().getName() + " - " + ex.getMessage());
		}
		
		// Load data from the database
		Connection c = null;
		try {
			c = _jdbcPool.getConnection();

			// Load time zones
			log.info("Loading Time Zones");
			GetTimeZone dao = new GetTimeZone(c);
			log.info("Loaded " + dao.initAll() + " Time Zones");
			
			// Load country codes
			log.info("Loading Country codes");
			GetCountry cdao = new GetCountry(c);
			log.info("Loaded " + cdao.initAll() + " Country codes");

			// Load Database information
			log.info("Loading Cross-Application data");
			GetUserData uddao = new GetUserData(c);
			SystemData.add("apps", uddao.getAirlines(true));

			// Load active airlines
			log.info("Loading Airline Codes");
			GetAirline aldao = new GetAirline(c);
			SystemData.add("airlines", aldao.getAll());

			// Load airports
			log.info("Loading Airports");
			GetAirport apdao = new GetAirport(c);
			Map<String, Airport> airports = apdao.getAll(); 
			SystemData.add("airports", airports);
			ETOPSHelper.init(airports.values());
			log.info("Initialized ETOPS helper");
			
			// Load MVS voice channels if enabled
			if (SystemData.getBoolean("acars.voice.enabled")) {
				GetMVSChannel chdao = new GetMVSChannel(c);
				Collection<Channel> channels = chdao.getAll();
				VoiceChannels vc = VoiceChannels.getInstance();
				for (Channel ch : channels)
					vc.add(null, ch);
				
				log.info("Loaded " + channels.size() + " persistent Voice channels");
				SharedData.addData(SharedData.MVS_POOL, vc);
			}
			
			// Load TS2 server info if enabled
			if (SystemData.getBoolean("airline.voice.ts2.enabled")) {
				SetTS2Data ts2wdao = new SetTS2Data(c);
				int flagsCleared = ts2wdao.clearActiveFlags();
				if (flagsCleared > 0)
					log.warn("Reset " + flagsCleared + " TeamSpeak 2 client activity flags");
			}
		} catch (Exception ex) {
			log.error("Error retrieving data - " + ex.getMessage(), ex);
		} finally {
			_jdbcPool.release(c);
		}
		
		// Start the ACARS/Mailer/IPC daemons
		TomcatDaemon tcDaemon = new TomcatDaemon();
		spawnDaemon(tcDaemon);
		spawnDaemon(new MailerDaemon());
		spawnDaemon(new IPCDaemon());
		
		// Save the ACARS daemon and client version map
		SharedData.addData(SharedData.ACARS_DAEMON, tcDaemon);
		
		// Wait a bit for the daemons to spool up
		ThreadUtils.sleep(1000);
	}
	
	/*
	 * Helper method to spawn a system daemon.
	 */
	private void spawnDaemon(Runnable sd) {
		Thread dt = new Thread(_dGroup, sd, sd.toString());
		dt.setDaemon(true);
		dt.setUncaughtExceptionHandler(this);
		synchronized (_daemons) {
			_daemons.put(dt, sd);
			dt.start();
		}
	}

	/**
	 * Uncaught exception handler.
	 */
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		Runnable r = _daemons.get(t);
		if (r == null) {
			log.warn("Unknown Thread - " + t.getName());
			return;
		}
		
		// Log the error
		log.error(e.getMessage(), e);
		
		// Spawn a new daemon
		Thread nt = new Thread(r, r.toString());
		nt.setDaemon(true);
		nt.setUncaughtExceptionHandler(this);
		synchronized (_daemons) {
			_daemons.remove(t);
			_daemons.put(nt, r);
			nt.start();
		}
	}
}