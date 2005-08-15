package org.deltava.acars.workers;

import java.util.*;
import java.io.IOException;

import java.net.*;
import java.nio.channels.*;

import org.deltava.acars.*;
import org.deltava.acars.beans.*;

import org.deltava.acars.message.RawMessage;
import org.deltava.acars.message.QuitMessage;

import org.deltava.acars.xml.MessageWriter;
import org.deltava.acars.xml.XMLException;

import org.deltava.util.IDGenerator;
import org.deltava.util.system.SystemData;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public final class NetworkHandler extends Worker {

	// System hello message
	private static final String SYSTEM_HELLO = ACARSInfo.APP_NAME + " v" + String.valueOf(ACARSInfo.MAJOR_VERSION)
			+ "." + String.valueOf(ACARSInfo.MINOR_VERSION) + " HELLO";

	private static ACARSConnectionPool _pool;
	private static Selector _cSelector;
	private static ServerSocketChannel _channel;

	private MessageStack _fmtInputStack;

	public NetworkHandler() {
		super("Network I/O Handler", NetworkHandler.class);
	}

	public void setFormattedInputStack(MessageStack stack) {
		_fmtInputStack = stack;
	}

	private void newConnection(SocketChannel sc) {

		// Create a new connection bean
		ACARSConnection con = new ACARSConnection(IDGenerator.generate(), sc);
		ServerStats.add(ServerStats.CONNECT_COUNT);

		// Check if we have a connection from there already
		if (!SystemData.getBoolean("acars.pool.multiple")) {
			if (_pool.hasConnection(con.getRemoteAddr())) {
				try {
					sc.close();
				} catch (IOException ie) {
					log.error("Cannot close socket - " + ie.getMessage());
				} finally {
					log.warn("Duplicate connection from " + con.getRemoteAddr());
				}

				return;
			}
		}

		// Get the socket and set various socket options
		Socket s = sc.socket();
		try {
			s.setSoLinger(true, 2);
			s.setTcpNoDelay(true);
		} catch (SocketException se) {
			log.error("Error setting socket options - " + se.getMessage());
		}

		// Register the channel with the selector and the message writer/dispatcher
		try {
			_pool.add(con);
			MessageWriter.addConnection(con.getID(), null, 1);
			log.info("New Connection from " + con.getRemoteAddr() + " - " + con.getFormatID());
		} catch (ACARSException ae) {
			log.error("Error adding to pool - " + ae.getMessage(), ae);
		} catch (XMLException xe) {
			log.error("Unable to register " + con.getFormatID() + " with dispatcher - " + xe.getMessage(), xe);
		}

		// Say hello
		con.write(new RawMessage(SYSTEM_HELLO + " " + con.getRemoteAddr() + "\r\n"));

		// Update the max/current connection counts
		ServerStats.add(ServerStats.CURRENT_CONNECT);
		long maxConnect = ServerStats.get(ServerStats.MAX_CONNECT);
		long curConnect = ServerStats.get(ServerStats.CURRENT_CONNECT);
		if (maxConnect < curConnect)
			ServerStats.set(ServerStats.MAX_CONNECT, curConnect);
	}

	public final void open() {

		// Call the parent open()
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

		// Close all of the worker threads
		for (Iterator i = _pool.getAll().iterator(); i.hasNext();) {
			ACARSConnection con = (ACARSConnection) i.next();
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
			_cSelector.select(SystemData.getInt("acars.sleep"));

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

			// Check if there are any messages waiting, and pushes them onto the raw input stack.
			_pool.read();

			// Check for inactive connections - generate a QUIT message for every one
			for (Iterator ic = _pool.checkConnections().iterator(); ic.hasNext();) {
				ACARSConnection con = (ACARSConnection) ic.next();
				log.info("Connection " + con.getFormatID() + " (" + con.getRemoteAddr() + ") disconnected");
				MessageWriter.remove(con.getID());
				if (con.isAuthenticated()) {
					log.debug("QUIT Message from " + con.getUser().getName());
					_fmtInputStack.push(new Envelope(new QuitMessage(con.getUser()), con.getID()));
				}
			}

			// Wake up threads waiting for stuff on the input stack
			_inStack.wakeup();

			// Dump stuff from the output queue to the sockets
			_pool.write();
		}

		// Mark the interrupt
		log.info("Interrupted");
	}
}