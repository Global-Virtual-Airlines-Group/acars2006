// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2014 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.sql.Connection;
import java.util.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.QuitMessage;

import org.deltava.dao.acars.SetConnection;

import org.deltava.util.*;
import org.deltava.util.system.SystemData;

import org.gvagroup.ipc.WorkerStatus;
import org.gvagroup.jdbc.ConnectionPool;

/**
 * An ACARS Server task to handle reading from network connections.
 * @author Luke
 * @version 5.4
 * @since 1.0
 */

public class NetworkReader extends Worker {
	
	private ConnectionPool _cPool;

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
		_cPool = (ConnectionPool) SystemData.getObject(SystemData.JDBC_POOL);
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
		Collection<Long> conIDs = new HashSet<Long>();
		_status.setMessage("Closing connections");
		for (Iterator<ACARSConnection> i = _pool.getAll().iterator(); i.hasNext();) {
			ACARSConnection con = i.next();
			if (con.isAuthenticated()) {
				log.warn("Disconnecting " + con.getUser().getPilotCode() + " (" + con.getRemoteAddr() + ")");
				conIDs.add(new Long(con.getID()));
			} else
				log.warn("Disconnecting (" + con.getRemoteAddr() + ")");

			// Close the connection and remove from the worker threads
			con.close();
			i.remove();
		}
		
		// Log connection close
		logCloseConnections(conIDs);
		super.close();
	}

	/**
	 * Executes the Thread.
	 */
	@Override
	public void run() {
		log.info("Started");
		_status.setStatus(WorkerStatus.STATUS_START);
		long lastExecTime = 0; int sleepTime = SystemData.getInt("acars.sleep", 30000);

		while (!Thread.currentThread().isInterrupted()) {
			_status.setMessage("Waiting for Data");
			_status.execute();
			int consWaiting = 0;
			try {
				long runInterval = System.currentTimeMillis() - lastExecTime;
				if (runInterval < 50)
					Thread.sleep(50 - runInterval);
				
				consWaiting = _pool.select(sleepTime);
			} catch (InterruptedException ie) {
				log.warn("Interrupted");
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				log.warn("Error on select - " + e.getMessage(), e);
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
					log.warn("Excessive select time - " + (selectTime - lastExecTime) + "ms (" + _pool.size() + " connections)");
				
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
						log.info("Connection " + StringUtils.formatHex(con.getID()) + " (" + con.getRemoteAddr() + ") disconnected");
						if (con.isAuthenticated()) {
							if (log.isDebugEnabled())
								log.debug("QUIT Message from " + con.getUser().getName());
								
							conIDs.add(Long.valueOf(con.getID()));
							QuitMessage qmsg = new QuitMessage(con.getUser());
							qmsg.setFlightID(con.getFlightID());
							qmsg.setHidden(con.getUserHidden());
							qmsg.setDispatch(con.getIsDispatch());
							qmsg.setMP(con.getIsMP());
							qmsg.setVoice(con.isVoiceEnabled() || con.getMuted());
							MSG_INPUT.add(new MessageEnvelope(qmsg, con.getID()));
						}
					}
					
					// Save the end times
					logCloseConnections(conIDs);
					
					// Check execution time
					long execTime = System.currentTimeMillis() - selectTime;
					if (execTime > 2500)
						log.warn("Excessive disconnection logging time - " + execTime + "ms (" + _pool.size() + " connections)");
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
		Connection con = null;
		try {
			con = _cPool.getConnection();
			SetConnection dao = new SetConnection(con);
			dao.closeConnections(ids);
		} catch (Exception e) {
			log.error("Error logging closed Connections - " + e.getMessage(), e);
		} finally {
			_cPool.release(con);
		}
	}
}