// Copyright 2007, 2008, 2009, 2010, 2011, 2013, 2015, 2016, 2017, 2018, 2019, 2021, 2023, 2024, 2025 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars;

import java.io.*;
import java.sql.*;
import java.util.*;

import jakarta.servlet.*;

import org.apache.logging.log4j.*;

import org.deltava.dao.*;
import org.deltava.mail.MailerDaemon;
import org.deltava.acars.ipc.IPCDaemon;

import org.deltava.beans.flight.ETOPSHelper;
import org.deltava.beans.navdata.Airspace;
import org.deltava.beans.schedule.Airport;

import org.deltava.security.Authenticator;

import org.deltava.util.*;
import org.deltava.util.jmx.*;
import org.deltava.util.cache.CacheLoader;
import org.deltava.util.system.*;

import org.gvagroup.common.SharedData;
import org.gvagroup.pool.*;
import org.gvagroup.tomcat.SharedWorker;

import com.newrelic.api.agent.NewRelic;

/**
 * A servlet context listener to spawn ACARS in its own J2EE web application.
 * @author Luke
 * @version 12.3
 * @since 1.0
 */

public class SystemBootstrap implements ServletContextListener, Thread.UncaughtExceptionHandler {
	
	private static final Logger log = LogManager.getLogger(SystemBootstrap.class);
	
	private JDBCPool _jdbcPool;
	private JedisPool _jedisPool;
	private final Map<Thread, Runnable> _daemons = new HashMap<Thread, Runnable>();

	@Override
	public void contextDestroyed(ServletContextEvent e) {
		Collection<Thread> threads = new ArrayList<Thread>(_daemons.keySet());
		_daemons.clear();
		threads.forEach(Thread::interrupt);
		SharedWorker.clear(Thread.currentThread().getContextClassLoader());
		
		// Shut down the JDBC connection pool
		ThreadUtils.kill(threads, 2500);
		JMXUtils.clear();
		_jdbcPool.close();
		
		// Clear shared data
		String code = SystemData.get("airline.code");
		SharedData.purge(code);
		log.error("Shut down {}", code);
	}
	
	@Override
	public void contextInitialized(ServletContextEvent e) {
		TaskTimer tt = new TaskTimer();
		e.getServletContext().setAttribute("startedOn", java.time.Instant.now());
		
		// Initialize system data
		SystemData.init();
		String code = SystemData.get("airline.code");
		log.info("Starting {}", code);
		SharedData.addApp(code);
		
		// Load Secrets
		if (SystemData.has("security.secrets")) {
			try {
				SecretManager sm = new PropertiesSecretManager(SystemData.get("security.secrets"));
				sm.load();
				sm.getKeys().forEach(k -> SystemData.add(k, sm.get(k)));
				log.info("Loaded {} secrets from {}", Integer.valueOf(sm.size()), SystemData.get("security.secrets"));
			} catch (IOException ie) {
				log.atError().withThrowable(ie).log("Error loading secrets - {}", ie.getMessage());
			}
		}
		
		// Initialize the Jedis connection pool
		log.info("Starting Jedis connection pool");
		_jedisPool = new JedisPool(SystemData.getInt("jedis.pool_max_size", 2), code);
		_jedisPool.setProperties((Map<?, ?>) SystemData.getObject("jedis.connectProperties"));
		_jedisPool.setLogStack(SystemData.getBoolean("jedis.log_stack"));
		try {
			_jedisPool.connect(SystemData.getInt("jedis.pool_size"));
			JedisUtils.init(_jedisPool);
			JMXConnectionPool jmxpool = new JMXConnectionPool(code, _jedisPool);
			JMXUtils.register("org.gvagroup:type=JedisPool,name=" + code, jmxpool);
			SharedWorker.register(new JMXRefreshTask(jmxpool, 60000));
		} catch (ConnectionPoolException cpe) {
			Throwable t = cpe.getCause();
			log.atError().withThrowable(t).log("Error connecting to Jedis - {}", t.getMessage());
		}
		
		// Save the connection pool in the SystemData
		SystemData.add(SystemData.JEDIS_POOL, _jedisPool);
		SharedData.addData(SharedData.JEDIS_POOL + SystemData.get("airline.code"), _jedisPool);
		
		// Initialize the connection pool
		log.info("Starting JDBC connection pool");
		_jdbcPool = new JDBCPool(SystemData.getInt("jdbc.pool_max_size", 2), SystemData.get("airline.code"));
		_jdbcPool.setProperties((Map<?, ?>) SystemData.getObject("jdbc.connectProperties"));
		_jdbcPool.setCredentials(SystemData.get("jdbc.user"), SystemData.get("jdbc.pwd"));
		_jdbcPool.setURL(SystemData.get("jdbc.url"));
		_jdbcPool.setMaxRequests(SystemData.getInt("jdbc.max_reqs", 0));
		_jdbcPool.setLogStack(SystemData.getBoolean("jdbc.log_stack"));
		
		// Attempt to load the driver and connect
		try {
			_jdbcPool.setDriver(SystemData.get("jdbc.driver"));
			_jdbcPool.setSocket(SystemData.get("jdbc.socket"));
			_jdbcPool.connect(SystemData.getInt("jdbc.pool_size"));
			JMXConnectionPool jmxpool = new JMXConnectionPool(code, _jdbcPool);
			JMXUtils.register("org.gvagroup:type=JDBCPool,name=" + code, jmxpool);
			SharedWorker.register(new JMXRefreshTask(jmxpool, 60000));
		} catch (ClassNotFoundException cnfe) {
			log.error("Cannot load JDBC driver class - {}", SystemData.get("jdbc.Driver"));
		} catch (ConnectionPoolException cpe) {
			log.error("Error connecting to JDBC data source - {}", cpe.getCause().getMessage(), cpe.getCause());
		}
		
		// Save the connection pool in the SystemData
		SystemData.add(SystemData.JDBC_POOL, _jdbcPool);
		SharedData.addData(SharedData.JDBC_POOL + SystemData.get("airline.code"), _jdbcPool);
		
		// Initialize caches
		try (InputStream is = ConfigLoader.getStream("/etc/cacheInfo.xml")) {
			CacheLoader.load(is);
		} catch(IOException ie) {
			log.warn("Cannot configure caches from code - {}", ie.getMessage());
		}

		// Load caches into JMX
		JMXCacheManager cm = new JMXCacheManager(code);
		JMXUtils.register("org.gvagroup:type=CacheManager,name=" + code, cm);
		SharedWorker.register(new JMXRefreshTask(cm, 60000));
		
		// Get and load the authenticator
		String authClass = SystemData.get("security.auth");
		try {
			Class<?> c = Class.forName(authClass);
			Authenticator auth = (Authenticator) c.getDeclaredConstructor().newInstance();
			log.debug("Loaded class {}", authClass);

			// Initialize and store in the servlet context
			auth.init(Authenticator.DEFAULT_PROPS_FILE);
			SystemData.add(SystemData.AUTHENTICATOR, auth);
		} catch (ClassNotFoundException cnfe) {
			log.error("Cannot find authenticator class {}", authClass);
		} catch (SecurityException se) {
			log.error("Error initializing authenticator - {}", se.getMessage());
		} catch (Exception ex) {
			log.error("Error starting authenticator - {} - {}", ex.getClass().getName(), ex.getMessage());
		}
		
		// Load data from the database
		try (Connection c = _jdbcPool.getConnection()) {
			// Load time zones
			log.info("Loading Time Zones");
			GetTimeZone dao = new GetTimeZone(c);
			dao.initAll();
			log.info("Loaded Time Zones");
			
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
			
			// Load prohibited airspace
			log.info("Loading restricted Airspace");
			GetAirspace asdao = new GetAirspace(c);
			Airspace.init(asdao.getRestricted());
		} catch (Exception ex) {
			log.atError().withThrowable(ex).log("Error retrieving data - {}", ex.getMessage());
		}
		
		// Start the ACARS/Mailer/IPC daemons
		TomcatDaemon tcDaemon = new TomcatDaemon();
		tcDaemon.initACARSConnectionPool(); // Ensure the connection pool is created before IPCDaemon starts
		spawnDaemon(tcDaemon);
		spawnDaemon(new MailerDaemon());
		spawnDaemon(new IPCDaemon());
		
		// Save the ACARS daemon and client version map
		SharedData.addData(SharedData.ACARS_DAEMON, tcDaemon);
		log.warn("Started {} in {}ms", code, Long.valueOf(tt.stop()));
		
		// Wait a bit for the daemons to spool up
		ThreadUtils.sleep(225);
	}
	
	/*
	 * Helper method to spawn a system daemon.
	 */
	private void spawnDaemon(Runnable sd) {
		Thread dt = Thread.ofVirtual().name(sd.toString()).unstarted(sd);
		dt.setUncaughtExceptionHandler(this);
		synchronized (_daemons) {
			_daemons.put(dt, sd);
			dt.start();
		}
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		Runnable r = _daemons.get(t);
		if (r == null) {
			log.warn("Unknown Thread - {}", t.getName());
			return;
		}
		
		// Log the error
		log.atError().withThrowable(e).log(e.getMessage());
		NewRelic.noticeError(e, false);
		
		// Spawn a new daemon
		Thread nt = Thread.ofVirtual().name(r.toString()).unstarted(r);
		nt.setUncaughtExceptionHandler(this);
		synchronized (_daemons) {
			_daemons.remove(t);
			_daemons.put(nt, r);
			nt.start();
		}
	}
}