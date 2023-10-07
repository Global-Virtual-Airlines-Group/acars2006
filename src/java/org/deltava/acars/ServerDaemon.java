// Copyright 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016, 2017, 2020, 2021, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars;

import java.sql.Connection;

import java.util.*;

import org.apache.logging.log4j.*;

import org.deltava.acars.beans.ACARSConnectionPool;

import org.deltava.acars.workers.*;

import org.deltava.dao.*;

import org.deltava.security.Authenticator;
import org.deltava.util.system.SystemData;

import org.gvagroup.common.SharedData;
import org.gvagroup.jdbc.*;

/**
 * A class to support common ACARS Server daemon functions.
 * @author Luke
 * @version 11.1
 * @since 1.0
 */

public abstract class ServerDaemon implements Thread.UncaughtExceptionHandler {
	
	/**
	 * Maximum task execution time.
	 */
	protected static final long MAX_EXEC = 60000;
 	
	/**
	 * Task logger.
	 */
 	protected Logger log = null;
 	
 	// Worker tasks
 	protected final ThreadGroup _workers = new ThreadGroup("ACARS Workers");
 	protected final Map<Thread, Worker> _threads = new LinkedHashMap<Thread, Worker>();
 	
 	/**
 	 * The connection pool.
 	 */
 	protected ACARSConnectionPool _conPool;
 	
 	protected void initLog(Class<?> loggerClass) {
        log = LogManager.getLogger(loggerClass);
 	}
 	
 	protected void initAuthenticator() {
 		
        // Get and load the authenticator
        String authClass = SystemData.get("security.auth");
        try {
           Class<?> c = Class.forName(authClass);
           Authenticator auth = (Authenticator) c.getDeclaredConstructor().newInstance();
       	   log.debug("Loaded class " + authClass);
           
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
 	}

 	protected void initConnectionPool() {

 	    // Initialize the connection pool
 	    log.info("Starting JDBC connection pool");
 	    ConnectionPool jdbcPool = new ConnectionPool(SystemData.getInt("jdbc.pool_max_size", 2), SystemData.get("airline.code"));
 	    jdbcPool.setProperties((Map<?, ?>) SystemData.getObject("jdbc.connectProperties"));
 	    jdbcPool.setCredentials(SystemData.get("jdbc.user"), SystemData.get("jdbc.pwd"));
 	    jdbcPool.setURL(SystemData.get("jdbc.url"));
 	    
 	    // Attempt to load the driver and connect
 	    try {
 	        jdbcPool.setDriver(SystemData.get("jdbc.driver"));
 	        jdbcPool.connect(SystemData.getInt("jdbc.pool_size"));
 	    } catch (ClassNotFoundException cnfe) {
 	        log.error("Cannot load JDBC driver class - " + SystemData.get("jdbc.Driver"));
 	    } catch (ConnectionPoolException cpe) {
 	        log.error("Error connecting to JDBC data source - " + cpe.getCause().getMessage());
 	    }
 	    
 	    SystemData.add(SystemData.JDBC_POOL, jdbcPool);
 	}
 	
 	/**
 	 * Initializes the list of airports.
 	 */
 	protected void initAirports() {
 		
 		// Get the Connection Pool
 		ConnectionPool pool = (ConnectionPool) SystemData.getObject(SystemData.JDBC_POOL);
 		
 		Connection c = null;
 		try {
 			c = pool.getConnection();
            log.info("Loading Airports");
            GetAirport dao = new GetAirport(c);
            SystemData.add("airports", dao.getAll());
 		} catch (Exception de) {
 			log.error("Error loading Airports - " + de.getMessage());
 		} finally {
 			pool.release(c);
 		}
 	}
 	
 	/**
 	 * Initializes the connection pool.
 	 */
 	protected void initACARSConnectionPool() {
 		if (_conPool != null) return;
		_conPool = new ACARSConnectionPool(SystemData.getInt("acars.pool.size"));
		_conPool.setTimeout(SystemData.getInt("acars.timeout"));
		_conPool.setMaxSelects(SystemData.getInt("acars.pool.maxSelect", 15000));
		SharedData.addData(SharedData.ACARS_POOL, _conPool);
 	}
 	
 	/**
 	 * Initializes the worker threads.
 	 */
 	protected void initTasks() {
 		if (_conPool == null)
 			throw new IllegalStateException("No ACARS Connection Pool");
 		
 		// Create the task container and the thread group
 		List<Worker> tasks = new ArrayList<Worker>();
 		tasks.add(new ConnectionHandler());
 		tasks.add(new NetworkReader());
		tasks.add(new InputTranslator());
		tasks.add(new LogicProcessor());
		tasks.add(new OnlineStatusLoader());
		tasks.add(new GeoLocator());
		tasks.add(new OutputDispatcher());
		tasks.add(new BandwidthLogger());
		tasks.add(new NetworkWriter());

 		// Turn the workers into threads
 		for (Worker w : tasks) {
 			log.debug("Initializing " + w.getName());
 			w.setConnectionPool(_conPool);
 			w.open();
 			
 			Thread t = new Thread(_workers, w, w.getName());
 			t.setUncaughtExceptionHandler(this);
 			t.setDaemon(true);
 			_threads.put(t, w);
 			log.debug("Starting " + w.getName());
 			t.start();
 		}
 	}
 	
 	@Override
 	public void uncaughtException(Thread t, Throwable e) {
 		if (!_threads.containsKey(t)) {
 			log.warn("Unknown worker thread {}", t.getName());
 			return;
 		}
 		
 		// Log the error
 		log.error(t.getName() + " error: " + e.getMessage(), e);
 		
 		// Get the worker and remove it
 		Worker w = _threads.get(t);
 		_threads.remove(t);
 		
 		// Spwan a new thread
 		Thread wt = new Thread(_workers, w, w.getName());
 		wt.setUncaughtExceptionHandler(this);
 		_threads.put(wt, w);
 		wt.start();
 	}
}