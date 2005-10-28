// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars;

import java.sql.Connection;

import java.util.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.deltava.jdbc.*;

import org.deltava.acars.beans.ACARSConnectionPool;

import org.deltava.acars.workers.*;

import org.deltava.dao.GetAirport;
import org.deltava.dao.DAOException;

import org.deltava.security.Authenticator;
import org.deltava.util.system.SystemData;

/**
 * A class to support common ACARS Server daemon functions.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public abstract class ServerDaemon {
 	
 	protected static Logger log = null;
 	
 	// Worker tasks
 	protected static ThreadGroup _workers;
 	protected static List _tasks;
 	protected static Map _threads;
 	
 	protected static void initLog(Class loggerClass) {
 		PropertyConfigurator.configure("etc/log4j.properties");
        log = Logger.getLogger(loggerClass);
 	}
 	
 	protected static void initAuthenticator() {
 		
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

 	protected static void initConnectionPool() {

 	    // Initialize the connection pool
 	    log.info("Starting JDBC connection pool");
 	    ConnectionPool jdbcPool = new ConnectionPool(SystemData.getInt("jdbc.pool_max_size"));
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
 	
 	protected static void initAirports() {
 		
 		// Get the Connection Pool
 		ConnectionPool _pool = (ConnectionPool) SystemData.getObject(SystemData.JDBC_POOL);
 		
 		Connection c = null;
 		try {
 			c = _pool.getConnection(true);
 			
 			// Load the airports
            log.info("Loading Airports");
            GetAirport dao = new GetAirport(c);
            SystemData.add("airports", dao.getAll());
 		} catch (DAOException de) {
 			log.error("Error loading Airports - " + de.getMessage());
 		} finally {
 			_pool.release(c);
 		}
 	}
 	
 	protected static void initACARSConnectionPool() throws ACARSException {
		ACARSConnectionPool cPool = new ACARSConnectionPool(SystemData.getInt("acars.pool.size"));
		cPool.setTimeout(SystemData.getInt("acars.timeout"));
		SystemData.add(SystemData.ACARS_POOL, cPool);
 	}
 	
 	protected static void initTasks() {
 		
 		// Create the task container and the thread group
 		_tasks = new ArrayList();

		// Init the input translator
		InputTranslator iTrans = new InputTranslator();
		_tasks.add(iTrans);

		// Init the output dispatcher
		OutputDispatcher oDispatch = new OutputDispatcher();
		_tasks.add(oDispatch);

		// Init the network handler
		NetworkHandler nHandler = new NetworkHandler();
		_tasks.add(nHandler);

		// Get the logic processor pool size
		int logicThreads = SystemData.getInt("acars.pool.threads");
		if (logicThreads < 1)
		   logicThreads = 1;
		
		// Init the logic processor pool
		for (int x = 0; x < logicThreads; x++) {
		   LogicProcessor lProcessor = new LogicProcessor(x);
		   _tasks.add(lProcessor);
		}

		// Try to init all of the worker threads
		for (Iterator i = _tasks.iterator(); i.hasNext(); ) {
			Worker w = (Worker) i.next();
			log.debug("Initializing " + w.getName());
			w.open();
		}
 	}
 	
 	protected static void initThreads() {
 	
 		// Set common priority for worker threads
 		_workers = new ThreadGroup("ACARS Workers");
 		_workers.setDaemon(true);
 		
 		// Turn the workers into threads
 		_threads = new HashMap();
 		for (Iterator i = _tasks.iterator(); i.hasNext(); ) {
 			Worker w = (Worker) i.next();
 			Thread t = new Thread(_workers, w, w.getName());
 			_threads.put(w.getClass(), t);
 			log.debug("Starting " + w.getName());
 			t.start();
 		}
 	}
}