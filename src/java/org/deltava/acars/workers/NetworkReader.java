// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2014, 2015, 2017, 2019, 2023, 2024 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.sql.Connection;
import java.util.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.QuitMessage;

import org.deltava.dao.acars.SetConnection;

import org.deltava.util.*;
import org.deltava.util.system.SystemData;

import org.gvagroup.ipc.WorkerState;
import org.gvagroup.pool.ConnectionPool;

/**
 * An ACARS Server task to handle reading from network connections.
 * @author Luke
 * @version 11.4
 * @since 1.0
 */

public class NetworkReader extends Worker {
	
	private static final int MIN_EXEC_INTERVAL = 25; // 25ms between intervals
	
	private ConnectionPool<Connection> _cPool;

	/**
	 * Initializes the Worker.
	 */
	public NetworkReader() {
		super("Network I/O Reader", 20, NetworkReader.class);
	}

	/**
	 * Initializes the worker.
	 */
	@Override
	public final void open() {
		super.open();
		_cPool = SystemData.getJDBCPool();
		try {
			_pool.updateSelector();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Shuts down the Worker. All existing connections to the server socket will be closed.
	 */
	@Override
	public final void close() {

		// Close all of the connections
		_status.setMessage("Closing connections");
		for (ACARSConnection con : _pool.getAll()) {
			if (con.isAuthenticated())
				log.warn("Disconnecting {} ({})", con.getUserID(), con.getRemoteAddr());
			else
				log.warn("Disconnecting ({})", con.getRemoteAddr());

			_pool.remove(con);
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
		long lastExecTime = 0; int sleepTime = SystemData.getInt("acars.sleep", 30000);

		while (!Thread.currentThread().isInterrupted()) {
			_status.setMessage("Waiting for Data - " + String.valueOf(_pool.getSelectCount()) + " selects");
			_status.execute();
			int consWaiting = 0;
			try {
				long runInterval = System.currentTimeMillis() - lastExecTime;
				if (runInterval < MIN_EXEC_INTERVAL)
					Thread.sleep(MIN_EXEC_INTERVAL - runInterval);
				
				consWaiting = _pool.select(sleepTime);
			} catch (InterruptedException ie) {
				log.warn("Interrupted");
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				log.atWarn().withThrowable(e).log("Error on select - {}" + e.getMessage());
			}
			
			// Wait in case we just added a new connection
			// I think making read() use the read lock resolves this
			lastExecTime = System.currentTimeMillis();

			// Check if there are any messages waiting, and push them onto the raw input stack.
			if (consWaiting > 0) {
				_status.setMessage("Reading Inbound Messages");
				Collection<TextEnvelope> msgs = _pool.read();
				
				// Do select time
				long selectTime = System.currentTimeMillis();
				if ((selectTime  - lastExecTime) > 1500)
					log.warn("Excessive select time - {}ms ({} connections)", Long.valueOf(selectTime - lastExecTime), Integer.valueOf(_pool.size()));
				
				// Write messages
				if (!msgs.isEmpty())
					RAW_INPUT.addAll(msgs);

				// Check for inactive connections - generate a QUIT message for every one
				Collection<ACARSConnection> disCon = _pool.checkConnections();
				if (!disCon.isEmpty()) {
					_status.setMessage("Handling disconnections");
					Collection<Long> conIDs = new HashSet<Long>();
					for (Iterator<ACARSConnection> ic = disCon.iterator(); ic.hasNext();) {
						ACARSConnection con = ic.next();
						log.info("Connection {} ({}) disconnected", StringUtils.formatHex(con.getID()), con.getRemoteAddr());
						if (con.isAuthenticated()) {
							log.debug("QUIT Message from {}", con.getUser().getName());
							conIDs.add(Long.valueOf(con.getID()));
							MSG_INPUT.add(new MessageEnvelope(new QuitMessage(con), con.getID()));
						}
					}
					
					// Save the end times
					logCloseConnections(conIDs);
					
					// Check execution time
					long execTime = System.currentTimeMillis() - selectTime;
					if (execTime > 2500)
						log.warn("Excessive disconnection logging time - {}ms ({} connections)", Long.valueOf(execTime), Integer.valueOf(_pool.size()));
				}
			}
			
			_status.complete();
		}
	}
	
	/*
	 * Helper method to log closing of connections.
	 */
	private void logCloseConnections(Collection<Long> ids) {
		if (ids.isEmpty()) return;
		_status.setMessage("Loggng Closed Connections");
		try (Connection con = _cPool.getConnection()) {
			SetConnection dao = new SetConnection(con);
			dao.closeConnections(ids);
		} catch (Exception e) {
			log.atError().withThrowable(e).log("Error logging closed Connections - {}", e.getMessage());
		}
	}
}