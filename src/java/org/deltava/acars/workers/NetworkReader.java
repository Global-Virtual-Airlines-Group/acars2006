// Copyright 2004, 2005, 2006, 2007, 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.*;
import java.io.IOException;
import java.nio.channels.Selector;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.QuitMessage;

import org.deltava.util.*;
import org.deltava.util.system.SystemData;

/**
 * An ACARS Server task to handle reading from network connections.
 * @author Luke
 * @version 2.1
 * @since 1.0
 */

public final class NetworkReader extends Worker {
	
	private Selector _cSelector;

	/**
	 * Initializes the Worker.
	 */
	public NetworkReader() {
		super("Network I/O Reader", 20, NetworkReader.class);
	}

	/**
	 * Initializes the worker.
	 */
	public final void open() {
		super.open();
		
		// Create the selector and attach it to the connection pool
		try {
			_cSelector = Selector.open();
			_pool.setSelector(_cSelector);
		} catch (IOException ie) {
			log.error(ie.getMessage());
			throw new IllegalStateException(ie.getMessage());
		}
	}

	/**
	 * Shuts down the Worker. All existing connections to the server socket will be closed.
	 */
	public final void close() {

		// Close all of the connections
		_status.setMessage("Closing connections");
		for (Iterator<ACARSConnection> i = _pool.get("*").iterator(); i.hasNext();) {
			ACARSConnection con = i.next();
			if (con.isAuthenticated())
				log.warn("Disconnecting " + con.getUser().getPilotCode() + " (" + con.getRemoteAddr() + ")");
			else
				log.warn("Disconnecting (" + con.getRemoteAddr() + ")");

			// Close the connection and remove from the worker threads
			con.close();
			i.remove();
		}

		// Call the superclass close
		super.close();
	}

	/**
	 * Executes the Thread.
	 */
	public void run() {
		log.info("Started");
		_status.setStatus(WorkerStatus.STATUS_START);

		while (!Thread.currentThread().isInterrupted()) {
			// Check for some data using our timeout value
			_status.setMessage("Waiting for Data");
			_status.execute();
			int consWaiting = 0;
			try {
				consWaiting = _cSelector.select(SystemData.getInt("acars.sleep"));
			} catch (Exception e) {
				log.warn("Error on select - " + e.getMessage());
			}
			
			// Wait in case we just added a new connection
			long startTime = 0;
			synchronized (_pool) {
				startTime = System.currentTimeMillis();
			}

			// Check if there are any messages waiting, and push them onto the raw input stack.
			if (consWaiting > 0) {
				_status.setMessage("Reading Inbound Messages");
				Collection<TextEnvelope> msgs = _pool.read();
				if (!msgs.isEmpty())
					RAW_INPUT.addAll(msgs);

				// Check for inactive connections - generate a QUIT message for every one
				Collection<ACARSConnection> disCon = _pool.checkConnections();
				if (!disCon.isEmpty()) {
					_status.setMessage("Handling disconnections");
					for (Iterator<ACARSConnection> ic = disCon.iterator(); ic.hasNext();) {
						ACARSConnection con = ic.next();
						log.info("Connection " + StringUtils.formatHex(con.getID()) + " (" + con.getRemoteAddr() + ") disconnected");
						if (con.isAuthenticated()) {
							if (log.isDebugEnabled());
								log.debug("QUIT Message from " + con.getUser().getName());
								
							QuitMessage qmsg = new QuitMessage(con.getUser());
							qmsg.setFlightID(con.getFlightID());
							qmsg.setHidden(con.getUserHidden());
							qmsg.setDispatch(con.getIsDispatch());
							qmsg.setMP(con.getIsMP());
							MSG_INPUT.add(new MessageEnvelope(qmsg, con.getID()));
						}
					}
				}
			}
			
			// Check execution time
			long execTime = System.currentTimeMillis() - startTime;
			if (execTime > 2250)
				log.warn("Excessive read time - " + execTime + "ms (" + _pool.size() + " connections)");
			
			// Log executiuon
			_status.complete();
		}
	}
}