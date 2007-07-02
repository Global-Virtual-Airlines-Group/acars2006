// Copyright 2004, 2005, 2006, 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.*;
import java.io.IOException;

import java.net.*;
import java.nio.channels.*;

import org.deltava.acars.*;
import org.deltava.acars.beans.*;

import org.deltava.acars.message.QuitMessage;
import org.deltava.acars.security.UserBlocker;

import org.deltava.beans.system.VersionInfo;

import org.deltava.util.*;
import org.deltava.util.system.SystemData;

/**
 * An ACARS Server task to handle reading from network connections.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public final class NetworkReader extends Worker {

	// System hello message
	private static final String SYSTEM_HELLO = "ACARS " + VersionInfo.APPNAME + " HELLO";

	private Selector _cSelector;
	private ServerSocketChannel _channel;

	/**
	 * The name of the SystemData attribute to store blocked addresses.
	 */
	public static final String BLOCKADDR_LIST = "acars.pool.blockList";
	private Collection<String> _blockedAddrs;

	/**
	 * Initializes the Worker.
	 */
	public NetworkReader() {
		super("Network I/O Reader", NetworkReader.class);
	}

	private void newConnection(SocketChannel sc) {
		_status.setMessage("Opening connection from " + sc.socket().getInetAddress().getHostAddress());

		// Create a new connection bean
		ACARSConnection con = null;
		if (SystemData.getBoolean("acars.debug"))
			con = new ACARSDebugConnection(IDGenerator.generate(), sc);
		else
			con = new ACARSConnection(IDGenerator.generate(), sc);

		// Check if the address is on the block list
		if (_blockedAddrs.contains(con.getRemoteAddr()) || _blockedAddrs.contains(con.getRemoteHost())) {
			log.warn("Refusing connection from " + con.getRemoteHost() + " (" + con.getRemoteAddr() + ")");
			con.close();
			return;
		}
		
		// Check if the address is from a banned user
		if (UserBlocker.isBanned(con.getRemoteAddr())) {
			log.warn("Refusing connection from banned user " + con.getRemoteHost() + " (" + con.getRemoteAddr() + ")");
			con.close();
			return;
		}
		
		// Check if we have a connection from there already
		if (!SystemData.getBoolean("acars.pool.multiple")) {
			ACARSConnection oldCon = _pool.getFrom(con.getRemoteAddr());
			boolean killOld = SystemData.getBoolean("acars.pool.kill_old");
			if ((oldCon != null) && !killOld) {
				con.close();
				log.warn("Duplicate connection from " + con.getRemoteAddr());
				return;
			} else if (oldCon != null) {
				log.warn("Closing original connection from " + con.getRemoteAddr());
				_pool.remove(oldCon);
			}
		}

		// Get the socket and set various socket options
		Socket s = sc.socket();
		try {
			s.setSoLinger(true, 1);
			s.setSendBufferSize(SystemData.getInt("acars.buffer.send"));
			s.setReceiveBufferSize(SystemData.getInt("acars.buffer.recv"));
		} catch (SocketException se) {
			log.error("Error setting socket options - " + se.getMessage(), se);
		}

		// Register the channel with the selector
		log.info("New Connection from " + con.getRemoteAddr());
		try {
			_pool.add(con);
			con.queue(SYSTEM_HELLO + " " + con.getRemoteAddr() + "\r\n");
		} catch (ACARSException ae) {
			log.error("Error adding to pool - " + ae.getMessage(), ae);
		}
	}

	@SuppressWarnings("unchecked")
	public final void open() {
		super.open();
		
		// Load the list of blocked connections
		_blockedAddrs = new HashSet<String>((Collection) SystemData.getObject("acars.block"));
		SystemData.add(BLOCKADDR_LIST, _blockedAddrs);

		// Init the server socket
		try {
			// Create the selector and attach it to the connection pool
			_cSelector = Selector.open();
			_pool.setSelector(_cSelector);

			// Open the socket channel
			_channel = ServerSocketChannel.open();
			ServerSocket socket = _channel.socket();
			_channel.configureBlocking(false);

			// Set the default receive buffer
			socket.setReceiveBufferSize(SystemData.getInt("acars.buffer.recv"));

			// Bind to the port
			SocketAddress sAddr = new InetSocketAddress(SystemData.getInt("acars.port"));
			socket.setReuseAddress(true);
			socket.bind(sAddr);

			// Add the server socket channel to the selector
			_channel.register(_cSelector, SelectionKey.OP_ACCEPT);
		} catch (IOException ie) {
			log.error(ie.getMessage());
			throw new IllegalStateException(ie.getMessage());
		}
	}

	/**
	 * Shuts down the Worker. All existing connections the server socket will be closed.
	 * @see Worker#close() 
	 */
	public final void close() {

		// Close all of the connections
		_status.setMessage("Closing connections");
		for (Iterator<ACARSConnection> i = _pool.getAll().iterator(); i.hasNext();) {
			ACARSConnection con = i.next();
			if (con.isAuthenticated())
				log.warn("Disconnecting " + con.getUser().getPilotCode() + " (" + con.getRemoteAddr() + ")");
			else
				log.warn("Disconnecting (" + con.getRemoteAddr() + ")");

			// Close the connection and remove from the worker threads
			con.close();
			i.remove();
		}

		// Try and close the server socket and the selector
		try {
			_channel.socket().close();
			_cSelector.close();
		} catch (IOException ie) {
			log.error(ie.getMessage());
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
			_status.setMessage("Listening for new Connection");
			_status.execute();
			try {
				_cSelector.select(SystemData.getInt("acars.sleep"));
			} catch (IOException ie) {
				log.warn("Error on select - " + ie.getMessage());
			}

			// See if we have someone waiting to connect
			SelectionKey ssKey = _channel.keyFor(_cSelector);
			if (ssKey.isAcceptable()) {
				try {
					SocketChannel cc = _channel.accept();
					if (cc != null)
						newConnection(cc);
				} catch (ClosedByInterruptException cie) {
				} catch (IOException ie) {
					log.error("Cannot accept connection - " + ie.getMessage(), ie);
					_status.setStatus(WorkerStatus.STATUS_ERROR);
					throw new RuntimeException("NetworkReader failure");
				}
			}

			// Check if there are any messages waiting, and push them onto the raw input stack.
			if (!_pool.isEmpty()) {
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
							log.debug("QUIT Message from " + con.getUser().getName());
							QuitMessage qmsg = new QuitMessage(con.getUser());
							qmsg.setFlightID(con.getFlightID());
							qmsg.setHidden(con.getUserHidden());
							MSG_INPUT.add(new MessageEnvelope(qmsg, con.getID()));
						}
					}
				}
			}
			
			// Log executiuon
			_status.complete();
		}
	}
}