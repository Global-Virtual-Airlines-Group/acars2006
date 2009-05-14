// Copyright 2005, 2006, 2007, 2008, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars;

import java.sql.Connection;

import java.util.*;

import org.apache.log4j.*;

import org.deltava.acars.beans.ACARSConnectionPool;

import org.deltava.acars.workers.*;

import org.deltava.jdbc.*;
import org.deltava.dao.*;

import org.deltava.security.Authenticator;
import org.deltava.util.system.SystemData;

import org.gvagroup.common.SharedData;

/**
 * A class to support common ACARS Server daemon functions.
 * @author Luke
 * @version 2.6
 * @since 1.0
 */

public abstract class ServerDaemon implements Thread.UncaughtExceptionHandler {
	
	/**
	 * Maximum task execution time.
	 */
	protected static final long MAX_EXEC = 60000;
 	
 	protected static Logger log = null;
 	
 	// Worker tasks
 	protected final ThreadGroup _workers = new ThreadGroup("ACARS Workers");
 	protected final Map<Thread, Worker> _threads = new LinkedHashMap<Thread, Worker>();
 	
 	protected ACARSConnectionPool _conPool;
 	
 	protected static void initLog(Class loggerClass) {
 		PropertyConfigurator.configure("etc/log4j.properties");
        log = Logger.getLogger(loggerClass);
 	}
 	
 	protected void initAuthenticator() {
 		
        // Get and load the authenticator
        String authClass = SystemData.get("security.auth");
        try {
           Class c = Class.forName(authClass);
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
 	}

 	protected void initConnectionPool() {

 	    // Initialize the connection pool
 	    log.info("Starting JDBC connection pool");
 	    ConnectionPool jdbcPool = new ConnectionPool(SystemData.getInt("jdbc.pool_max_size", 1));
 	    jdbcPool.setProperties((Map) SystemData.getObject("jdbc.connectProperties"));
 	    jdbcPool.setCredentials(SystemData.get("jdbc.user"), SystemData.get("jdbc.pwd"));
 	    jdbcPool.setProperty("url", SystemData.get("jdbc.url"));
 	    
 	    // Attempt to load the driver and connect
 	    try {
 	        jdbcPool.setDriver(SystemData.get("jdbc.driver"));
 	        jdbcPool.connect(SystemData.getInt("jdbc.pool_size"));
 	    } catch (ClassNotFoundException cnfe) {
 	        log.error("Cannot load JDBC driver class - " + SystemData.get("jdbc.Driver"));
 	    } catch (ConnectionPoolException cpe) {
 	        Throwable t = cpe.getCause();
 	        log.error("Error connecting to JDBC data source - " + t.getMessage());
 	    }
 	    
 	    // Save the connection pool in the SystemData
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
 		} catch (DAOException de) {
 			log.error("Error loading Airports - " + de.getMessage());
 		} finally {
 			pool.release(c);
 		}
 	}
 	
 	protected void initACARSConnectionPool() {
		_conPool = new ACARSConnectionPool(SystemData.getInt("acars.pool.size"));
		_conPool.setTimeout(SystemData.getInt("acars.timeout"));
		SharedData.addData(SharedData.ACARS_POOL, _conPool);
 	}
 	
 	protected void initTasks() {
 		if (_conPool == null)
 			throw new IllegalStateException("No ACARS Connection Pool");
 		
 		// Create the task container and the thread group
 		List<Worker> tasks = new ArrayList<Worker>();

		// Init the singleton workers
 		tasks.add(new ConnectionHandler());
 		tasks.add(new NetworkReader());
		tasks.add(new InputTranslator());
		tasks.add(new LogicProcessor());
		tasks.add(new OutputDispatcher());
		tasks.add(new BandwidthLogger());
		tasks.add(new NetworkWriter());

		// Try to init all of the worker threads
		for (Iterator<Worker> i = tasks.iterator(); i.hasNext(); ) {
			Worker w = i.next();
			log.debug("Initializing " + w.getName());
			w.setConnectionPool(_conPool);
			w.open();
		}
		
 		// Set common priority for worker threads
 		_workers.setDaemon(true);

 		// Turn the workers into threads
 		for (Iterator<Worker> i = tasks.iterator(); i.hasNext(); ) {
 			Worker w = i.next();
 			Thread t = new Thread(_workers, w, w.getName());
 			t.setUncaughtExceptionHandler(this);
 			_threads.put(t, w);
 			log.debug("Starting " + w.getName());
 			t.start();
 		}
 	}
 	
 	/**
 	 * Worker thread exception handler.
 	 */
 	public void uncaughtException(Thread t, Throwable e) {
 		if (!_threads.containsKey(t)) {
 			log.warn("Unknown worker thread " + t.getName());
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