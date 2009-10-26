// Copyright 2004, 2005, 2006, 2007, 2008, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.*;

import org.deltava.acars.ACARSException;
import org.deltava.acars.beans.*;

import org.deltava.beans.system.VersionInfo;

import org.deltava.util.IDGenerator;
import org.deltava.util.system.SystemData;

/**
 * An ACARS Server task to handle new network connections.
 * @author Luke
 * @version 2.7
 * @since 2.1
 */

public class ConnectionHandler extends Worker implements Thread.UncaughtExceptionHandler {
	
	private Selector _cSelector;
	private ServerSocketChannel _channel;

	/**
	 * The name of the SystemData attribute to store blocked addresses.
	 */
	public static final String BLOCKADDR_LIST = "acars.pool.blockList";
	
	/**
	 * The list of blocked addresses.
	 */
	protected final Collection<String> _blockedAddrs = new HashSet<String>();
	
	private class ConnectWorker implements Runnable {

		private final String SYSTEM_HELLO = "ACARS " + VersionInfo.APPNAME + " HELLO";

		private SocketChannel _sc;
		
		ConnectWorker(SocketChannel c) {
			super();
			_sc = c;
		}
		
		public void run() {
			
			// Create a new connection bean
			ACARSConnection con = null;
			if (SystemData.getBoolean("acars.debug"))
				con = new ACARSDebugConnection(IDGenerator.generate(), _sc);
			else
				con = new ACARSConnection(IDGenerator.generate(), _sc);

			// Check if the address is on the block list or from a banned user
			if (_blockedAddrs.contains(con.getRemoteAddr()) || _blockedAddrs.contains(con.getRemoteHost())) {
				log.warn("Refusing connection from " + con.getRemoteHost() + " (" + con.getRemoteAddr() + ")");
				con.close();
				return;
			}

			// Check if we have a connection from there already
			if (!SystemData.getBoolean("acars.pool.multiple")) {
				ACARSConnection oldCon = _pool.get(con.getRemoteAddr());
				boolean killOld = SystemData.getBoolean("acars.pool.kill_old");
				if ((oldCon != null) && oldCon.getIsDispatch())
					log.info("Duplicate connection from " + con.getRemoteAddr() + " dispatcher");
				else if ((oldCon != null) && !killOld) {
					con.close();
					log.warn("Duplicate connection from " + con.getRemoteAddr());
					return;
				} else if (oldCon != null) {
					oldCon.close();
					log.warn("Closing original connection from " + con.getRemoteAddr());
					_pool.remove(oldCon);
				}
			}

			// Get the socket and set various socket options
			try {
				Socket s = _sc.socket();
				s.setSoLinger(false, 0);
				s.setTcpNoDelay(true);
				s.setSendBufferSize(SystemData.getInt("acars.buffer.send"));
				s.setReceiveBufferSize(SystemData.getInt("acars.buffer.recv"));
			} catch (IOException ie) {
				log.error("Error setting socket options - " + ie.getMessage(), ie);
			}

			// Register the channel with the selector
			log.info("New Connection from " + con.getRemoteAddr());
			try {
				_pool.add(con);
				con.queue(SYSTEM_HELLO + " " + con.getRemoteAddr() + "\r\n");
			} catch (ACARSException ae) {
				log.error("Error adding to pool - " + ae.getMessage(), ae);
				con.close();
			}
		}
	}
	
	/**
	 * Initializes the Worker.
	 */
	public ConnectionHandler() {
		super("Connection Handler", 10, ConnectionHandler.class);
	}
	
	/**
	 * Initializes the worker.
	 */
	public final void open() {
		super.open();
		
		// Load the list of blocked connections
		_blockedAddrs.clear();
		SystemData.add(BLOCKADDR_LIST, _blockedAddrs);
		Collection<?> addrs = (Collection<?>) SystemData.getObject("acars.block");
		for (Iterator<?> i = addrs.iterator(); i.hasNext(); )
			_blockedAddrs.add((String) i.next());
		
		try {
			// Open the socket channel
			_cSelector = Selector.open();
			_channel = ServerSocketChannel.open();
			ServerSocket socket = _channel.socket();
			_channel.configureBlocking(false);

			// Bind to the port
			SocketAddress sAddr = new InetSocketAddress(SystemData.getInt("acars.port"));
			socket.setReceiveBufferSize(SystemData.getInt("acars.buffer.recv"));
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
	 * Shuts down the Worker.
	 */
	public final void close() {

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
	 * Uncaught exception handler for Connection workers.
	 */
	public void uncaughtException(Thread t, Throwable e) {
		log.error(t.getName() + " - " + e.getMessage(), e);
	}
	
	/**
	 * Executes the Thread.
	 */
	public void run() {
		log.info("Started");
		_status.setStatus(WorkerStatus.STATUS_START);
		
		while (!Thread.currentThread().isInterrupted()) {
			_status.setMessage("Listening for new Connection");
			_status.execute();
			try {
				_cSelector.select(SystemData.getInt("acars.sleep"));
			} catch (IOException ie) {
				log.warn("Error on select - " + ie.getMessage());
			}
			
			// See if we have someone waiting to connect
			SelectionKey ssKey = _channel.keyFor(_cSelector);
			if ((ssKey != null) && ssKey.isValid() && ssKey.isAcceptable()) {
				try {
					SocketChannel cc = _channel.accept();
					if (cc != null) {
						String addr = cc.socket().getInetAddress().getHostAddress();
						_status.setMessage("Opening connection from " + addr);
						ConnectWorker wrk = new ConnectWorker(cc);
						Thread wt = new Thread(wrk, "ConnectWorker-" + addr);
						wt.setDaemon(true);
						wt.setUncaughtExceptionHandler(this);
						wt.start();
					}
				} catch (ClosedByInterruptException cie) {
					// empty
				} catch (IOException ie) {
					log.error("Cannot accept connection - " + ie.getMessage(), ie);
					_status.setStatus(WorkerStatus.STATUS_ERROR);
					_status.complete();
					throw new RuntimeException("NetworkReader failure");
				}
			}
			
			// Log executiuon
			_status.complete();
		}
	}
}