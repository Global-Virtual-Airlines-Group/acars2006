// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.*;

import org.deltava.acars.ACARSException;
import org.deltava.acars.beans.*;
import org.deltava.beans.system.VersionInfo;
import org.deltava.util.*;
import org.deltava.util.system.SystemData;
import org.gvagroup.ipc.WorkerStatus;

/**
 * An ACARS Server task to handle new network connections.
 * @author Luke
 * @version 5.4
 * @since 2.1
 */

public class ConnectionHandler extends Worker implements Thread.UncaughtExceptionHandler {
	
	private Selector _cSelector;
	private int _selectCount;
	private ServerSocketChannel _channel;
	
	private final IDGenerator gen = new IDGenerator();

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

		private final SocketChannel _sc;
		private final long _id;
		
		ConnectWorker(SocketChannel c, long id) {
			super();
			_sc = c;
			_id = id;
		}
		
		@Override
		public void run() {
			
			// Create a new connection bean
			ACARSConnection con = null;
			if (SystemData.getBoolean("acars.debug"))
				con = new ACARSDebugConnection(_id, _sc);
			else
				con = new ACARSConnection(_id, _sc);

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
					log.info("Duplicate connection from " + con.getRemoteAddr() + " dispatcher (" + oldCon.getUserID() + ")");
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
				_sc.setOption(StandardSocketOptions.SO_LINGER, Integer.valueOf(1));
				_sc.setOption(StandardSocketOptions.TCP_NODELAY, Boolean.TRUE);
				_sc.setOption(StandardSocketOptions.SO_KEEPALIVE, Boolean.TRUE);
				_sc.setOption(StandardSocketOptions.SO_SNDBUF, Integer.valueOf(SystemData.getInt("acars.buffer.send")));
				_sc.setOption(StandardSocketOptions.SO_RCVBUF, Integer.valueOf(SystemData.getInt("acars.buffer.recv")));
			} catch (IOException ie) {
				log.error("Error setting socket options - " + ie.getMessage(), ie);
			}

			// Register the channel with the pool
			log.info("New Connection from " + con.getRemoteAddr());
			try {
				_pool.add(con);
				con.write(SYSTEM_HELLO + " " + con.getRemoteAddr() + "\r\n");
			} catch (ACARSException ae) {
				log.error("Error adding to pool - " + ae.getMessage(), ae);
				con.close();
			}
		}
		
		public long getID() {
			return _id;
		}
	}
	
	/**
	 * Creates the Worker.
	 */
	public ConnectionHandler() {
		super("Connection Handler", 10, ConnectionHandler.class);
	}
	
	/**
	 * Initializes the worker.
	 */
	@Override
	public final void open() {
		super.open();
		
		// Load the list of blocked connections
		_blockedAddrs.clear();
		SystemData.add(BLOCKADDR_LIST, _blockedAddrs);

		// Open the socket channel
		try {
			_cSelector = Selector.open();
			_channel = ServerSocketChannel.open();
			_channel.configureBlocking(false);
			_channel.setOption(StandardSocketOptions.SO_REUSEADDR, Boolean.TRUE);
			_channel.setOption(StandardSocketOptions.SO_RCVBUF, Integer.valueOf(SystemData.getInt("acars.buffer.recv")));
			_channel.bind(new InetSocketAddress(SystemData.getInt("acars.port")));
			_channel.register(_cSelector, SelectionKey.OP_ACCEPT);
		} catch (IOException ie) {
			log.error(ie.getMessage());
			throw new IllegalStateException(ie);
		}
	}
	
	/**
	 * Shuts down the Worker.
	 */
	@Override
	public final void close() {
		try {
			_channel.close();
			_cSelector.close();
		} catch (IOException ie) {
			log.error(ie.getMessage());
		}

		// Call the superclass close
		super.close();
	}
	
	/*
	 * Updates the accept selector.
	 */
	private void updateSelector() throws IOException {
		Selector s = Selector.open();
		_channel.register(s, SelectionKey.OP_ACCEPT);
		if (_cSelector != null)
			_cSelector.close();
		
		_cSelector = s;
		_selectCount = 0;
	}
	
	/**
	 * Uncaught exception handler for Connection workers.
	 */
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		log.error(t.getName() + " - " + e.getMessage(), e);
	}
	
	/**
	 * Executes the Thread.
	 */
	@Override
	public void run() {
		log.info("Started");
		_status.setStatus(WorkerStatus.STATUS_START);
		final int maxSelect = SystemData.getInt("acars.pool.maxSelect", 15000); final int sleepTime = SystemData.getInt("acars.sleep", 30000);
		
		while (!Thread.currentThread().isInterrupted()) {
			_selectCount++;
			_status.setMessage("Listening for new Connection - " + String.valueOf(_selectCount) + " selects");
			_status.execute();
			try {
				_cSelector.select(sleepTime);
				if (_selectCount > maxSelect)
					updateSelector();
			} catch (IOException ie) {
				log.warn("Error on select - " + ie.getMessage());
			}
			
			// See if we have someone waiting to connect
			SelectionKey ssKey = _channel.keyFor(_cSelector);
			if ((ssKey != null) && ssKey.isValid() && ssKey.isAcceptable()) {
				try {
					SocketChannel cc = _channel.accept();
					if (cc != null) {
						_status.setMessage("Opening connection from " + NetworkUtils.getSourceAddress(cc.getRemoteAddress()));
						gen.reset();
						ConnectWorker wrk = new ConnectWorker(cc, gen.generate());
						Thread wt = new Thread(wrk, "ConnectWorker-" + wrk.getID());
						wt.setDaemon(true);
						wt.setUncaughtExceptionHandler(this);
						wt.start();
					}
				} catch (ClosedChannelException cce) {
					// empty
				} catch (IOException ie) {
					log.error("Cannot accept connection - " + ie.getMessage(), ie);
					_status.setStatus(WorkerStatus.STATUS_ERROR);
					_status.complete();
					throw new RuntimeException("NetworkReader failure");
				}
			}
			
			_status.complete();
		}
	}
}