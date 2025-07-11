// Copyright 2011, 2015, 2016, 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import java.io.*;
import java.net.InetSocketAddress;

import java.nio.ByteBuffer;
import java.nio.channels.*;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

import org.deltava.beans.RemoteAddressBean;
import org.deltava.beans.acars.ConnectionStats;

import org.deltava.util.NetworkUtils;
import org.deltava.util.system.SystemData;

/**
 * An abstract class to store common NIO channel information.
 * @author Luke
 * @version 10.2
 * @since 4.0
 */

abstract class ACARSChannel<T> implements RemoteAddressBean, Serializable, Closeable {
	
	/**
	 * Maximum number of timed out write attempts.
	 */
	protected transient static final int MAX_WRITE_ATTEMPTS = 24;
	
	private transient final Lock _wLock = new ReentrantLock();
	private transient final Queue<T> _outBuffer = new ConcurrentLinkedQueue<T>();

	/**
	 * Remote address. 
	 */
	protected transient InetSocketAddress _remoteAddr;
	
	/**
	 * Write selector.
	 */
	protected transient Selector _wSelector;
	
	/**
	 * Output data buffer.
	 */
	protected transient final ByteBuffer _oBuffer = ByteBuffer.allocateDirect(SystemData.getInt("acars.buffer.nio"));
	
	/**
	 * Connection statistics.
	 */
	protected InternalConnectionStats _stats;
	
	private long _lastActivityTime;
	
	protected ACARSChannel(InetSocketAddress remoteAddr) {
		super();
		_remoteAddr = remoteAddr;
	}

	/**
	 * Closes the channel, clears the write buffer and releases the write selector.
	 */
	@Override
	public void close() {
		try {
			if (_wLock.tryLock(750, TimeUnit.MILLISECONDS)) {
				_outBuffer.clear();
				_wLock.unlock();
			}
		
			_wSelector.close();
		} catch (Exception e) {
			// empty
		}
	}
	
	/**
	 * Enqueues a message to be written.
	 * @param msg the message
	 */
	public void queue(T msg) {
		_outBuffer.add(msg);
		
		// Only allow one thread to write to the channel
		if (_wLock.tryLock()) {
			while (!_outBuffer.isEmpty())
				write(_outBuffer.poll());

			_wLock.unlock();
		}
	}
	
	/**
	 * Writes a message to the channel.
	 * @param msg the message
	 */
	protected abstract void write(T msg);
	
	/**
	 * Returns channel statistics.
	 * @return a ConnectionStats bean
	 */
	public ConnectionStats getStatistics() {
		return _stats;
	}
	
	public abstract SelectableChannel getChannel();
	
	@Override
	public String getRemoteAddr() {
		return _remoteAddr.getAddress().getHostAddress();
	}
	
	/**
	 * Returns the remote IP address and port.
	 * @return the address in addr:port format
	 */
	public String getAddress() {
		return NetworkUtils.getSourceAddress(_remoteAddr);
	}
	
	@Override
	public String getRemoteHost() {
		return _remoteAddr.getHostName();
	}
	
	/**
	 * Update last activity time.
	 */
	protected synchronized void updateLastActivity() {
		_lastActivityTime = System.currentTimeMillis();
	}
	
	/**
	 * Returns the last time data was sent or received on this channel.
	 * @return the last activity date/time
	 */
	public synchronized long getLastActivityTime() {
		return _lastActivityTime;
	}
}