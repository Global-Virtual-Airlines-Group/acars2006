// Copyright (c) 2004, 2005 Global Virtual Airline Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.*;
import java.io.IOException;

import java.net.*;
import java.nio.channels.*;

import org.deltava.acars.*;
import org.deltava.acars.beans.*;

import org.deltava.acars.message.QuitMessage;

import org.deltava.acars.xml.MessageWriter;
import org.deltava.acars.xml.XMLException;
import org.deltava.beans.acars.ServerStats;

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
	private static final String SYSTEM_HELLO = ACARSInfo.APP_NAME + " v" + String.valueOf(ACARSInfo.MAJOR_VERSION)
			+ "." + String.valueOf(ACARSInfo.MINOR_VERSION) + " HELLO";

	private ACARSConnectionPool _pool;
	private Selector _cSelector;
	private ServerSocketChannel _channel;

	public NetworkReader() {
		super("Network I/O Reader", NetworkReader.class);
	}

	private void newConnection(SocketChannel sc) {
		_status.setMessage("Opening connection from " + sc.socket().getInetAddress().getHostAddress());

		// Create a new connection bean
		ACARSConnection con = null;
		if (SystemData.getBoolean("acars.debug")) {
			con = new ACARSDebugConnection(IDGenerator.generate(), sc);
		} else {
			con = new ACARSConnection(IDGenerator.generate(), sc);
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
			s.setSoLinger(true, 2);
			s.setSendBufferSize(SystemData.getInt("acars.buffer.send"));
			s.setReceiveBufferSize(SystemData.getInt("acars.buffer.recv"));
		} catch (SocketException se) {
			log.error("Error setting socket options - " + se.getMessage(), se);
		}

		// Register the channel with the selector and the message writer/dispatcher
		try {
			_pool.add(con);
			MessageWriter.addConnection(con.getID(), null, 1);
			log.info("New Connection from " + con.getRemoteAddr());
		} catch (ACARSException ae) {
			log.error("Error adding to pool - " + ae.getMessage(), ae);
		} catch (XMLException xe) {
			log.error("Unable to register " + StringUtils.formatHex(con.getID()) + " with dispatcher - "
					+ xe.getMessage(), xe);
		}

		// Say hello
		con.write(SYSTEM_HELLO + " " + con.getRemoteAddr() + "\r\n");

		// Update the max/current connection counts
		ServerStats.connect();
	}

	public final void open() {
		super.open();

		// Get the ACARS Connection Pool
		_pool = (ACARSConnectionPool) SystemData.getObject(SystemData.ACARS_POOL);

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

	public final void close() {

		// Close all of the connections
		_status.setMessage("Closing connections");
		for (Iterator<ACARSConnection> i = _pool.getAll().iterator(); i.hasNext();) {
			ACARSConnection con = i.next();
			if (con.isAuthenticated()) {
				log.warn("Disconnecting " + con.getUser().getPilotCode() + " (" + con.getRemoteAddr() + ")");
			} else {
				log.warn("Disconnecting (" + con.getRemoteAddr() + ")");
			}

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

	protected void $run0() throws IOException {
		log.info("Started");

		while (!Thread.currentThread().isInterrupted()) {
			// Check for some data using our timeout value
			_status.setMessage("Listening for new Connection");
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
				}
			}

			// Check if there are any messages waiting, and push them onto the raw input stack.
			if (!_pool.isEmpty()) {
				_status.setMessage("Reading Inbound Messages");
				Collection<Envelope> msgs = _pool.read();
				if (!msgs.isEmpty()) {
					MessageStack.RAW_INPUT.push(msgs);
					MessageStack.RAW_INPUT.wakeup();
				}

				// Check for inactive connections - generate a QUIT message for every one
				Collection<ACARSConnection> disCon = _pool.checkConnections();
				if (!disCon.isEmpty()) {
					_status.setMessage("Handling disconnections");
					for (Iterator<ACARSConnection> ic = disCon.iterator(); ic.hasNext();) {
						ACARSConnection con = ic.next();
						ServerStats.disconnect();
						log.info("Connection " + StringUtils.formatHex(con.getID()) + " (" + con.getRemoteAddr()
								+ ") disconnected");
						MessageWriter.remove(con.getID());
						if (con.isAuthenticated()) {
							log.debug("QUIT Message from " + con.getUser().getName());
							QuitMessage qmsg = new QuitMessage(con.getUser());
							qmsg.setFlightID(con.getFlightID());
							MessageStack.MSG_INPUT.push(new Envelope(qmsg, con.getID()));
							MessageStack.MSG_INPUT.wakeup();
						}
					}
				}

				// Log executiuon
				_status.execute();
			}
		}

		// Mark the interrupt
		log.info("Interrupted");
	}
}