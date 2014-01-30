// Copyright 2011, 2014 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.net.*;
import java.util.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;

import org.deltava.acars.ACARSException;
import org.deltava.acars.beans.*;
import org.deltava.acars.message.VoiceMessage;

import org.deltava.util.NetworkUtils;
import org.deltava.util.system.SystemData;

import org.gvagroup.ipc.WorkerStatus;

/**
 * An ACARS worker thread to read voice packets.
 * @author Luke
 * @version 5.2
 * @since 4.0
 */

public class VoiceReader extends Worker {
	
	private Selector _rSelector;
	private final Collection<DatagramChannel> _channels = new ArrayList<DatagramChannel>();
	
	private final ByteBuffer _buf = ByteBuffer.allocateDirect(20480);
	
	/**
	 * Creates the Worker.
	 */
	public VoiceReader() {
		super("Voice Reader", 100, VoiceReader.class);
	}
	
	/**
	 * Initializes the Worker.
	 */
	@Override
	public final void open() {
		super.open();
		try {
			_rSelector = Selector.open();
			
			// Get channels
			for (Enumeration<NetworkInterface> ints = NetworkInterface.getNetworkInterfaces(); (ints != null) && ints.hasMoreElements(); ) {
				NetworkInterface ni = ints.nextElement();
				if (ni.isLoopback() || !ni.isUp() || ni.isVirtual()) {
					log.info("Skipping " + ni.getDisplayName() + ", loopback/virtual/down");
					continue;
				}
				
				log.info("Found " + ni.getDisplayName());
				for (Enumeration<InetAddress> addrs = ni.getInetAddresses(); addrs.hasMoreElements(); ) {
					InetAddress addr = addrs.nextElement();
					log.info("Binding to " + addr.getHostAddress());
				
					DatagramChannel ch = DatagramChannel.open();
					ch.configureBlocking(false);
					ch.setOption(StandardSocketOptions.SO_RCVBUF, Integer.valueOf(SystemData.getInt("acars.buffer.recv") * 2));
					ch.setOption(StandardSocketOptions.SO_SNDBUF, Integer.valueOf(SystemData.getInt("acars.buffer.send") * 4));
					ch.bind(new InetSocketAddress(addr, SystemData.getInt("acars.port")));
					ch.register(_rSelector, SelectionKey.OP_READ);
					_channels.add(ch);
				}
			}
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
			for (DatagramChannel ch : _channels)
				ch.close();
			
			_rSelector.close();
		} catch (IOException ie) {
			log.error(ie.getMessage());	
		} finally {
			_channels.clear();
		}
		
		super.close();
	}

	/**
	 * Executes the Thread.
	 */
	@Override
	public void run() {
		log.info("Started");
		_status.setStatus(WorkerStatus.STATUS_START);
		long lastExecTime = 0;
		while (!Thread.currentThread().isInterrupted()) {
			_status.setMessage("Listening for Voice packet");
			_status.execute();
			int consWaiting = 0;
			try {
				consWaiting = _rSelector.select(SystemData.getInt("acars.sleep", 30000));
			} catch (IOException ie) {
				log.warn("Error on select - " + ie.getMessage());
			}
			
			lastExecTime = System.currentTimeMillis();
			
			// See if we have some data
			if (consWaiting > 0) {
				_status.setMessage("Reading Inbound Voice Data");
				Collection<SelectionKey> keys = _rSelector.selectedKeys();
				for (SelectionKey k : keys) {
					DatagramChannel ch = (DatagramChannel) k.channel();
					try {
						InetSocketAddress srcAddr = (InetSocketAddress) ch.receive(_buf);
						while (srcAddr != null) {
							String addr = NetworkUtils.getSourceAddress(srcAddr);
							log.info("Received packet from " + addr + " on " + NetworkUtils.getSourceAddress(ch.getLocalAddress()));
							
							// Get the data
							byte[] pktData = new byte[_buf.flip().limit()];
							_buf.get(pktData);
							
							// This might return null if it's the first UDP packet since we don't know what
							// source port it's coming from. Do a lookup based on the IP address only. 
							ACARSConnection ac = _pool.get(addr);
							if (ac == null) {
								ac = _pool.get(srcAddr.getAddress().getHostAddress());
								if (ac == null) {
									if (pktData.length < 48) {
										_buf.flip();
										ch.send(_buf, srcAddr);
									}
									
									throw new IllegalArgumentException(addr + " - not connected" + ((pktData.length < 48) ? " (echo)" : ""));
								}
							} else if (!ac.isVoiceCapable() && (pktData.length > 47))
								throw new IllegalArgumentException(ac.getUserID() + " is not voice enabled");

							// Register the source/destination addresses
							ac.enableVoice(ch, srcAddr);
							try {
								_pool.add(ac);
							} catch (ACARSException ae) {
								log.error("Cannot register source address - " + ae.getMessage());
							}
							
							// Log the read
							ac.logVoice(pktData.length);
							
							// If it's a ping (ie. a 16-byte datagram), send it right back, otherwise push onto the queue
							if (pktData.length == 16) {
								log.info("Received MVS Ping from " + ac.getUserID());
								BinaryEnvelope oenv = new BinaryEnvelope(ac.getUser(), pktData, ac.getID());
								RAW_OUTPUT.add(oenv);
							} else {
								VoiceMessage msg = new VoiceMessage(ac.getUser(), pktData);
								MSG_INPUT.add(new MessageEnvelope(msg, ac.getID()));
							}
							
							srcAddr = (InetSocketAddress) ch.receive(_buf);
						}
					} catch (IllegalArgumentException iae) {
						log.error("Unexpected packet from " + iae.getMessage());
					} catch (IOException ie) {
						log.error("Error reading packet - " + ie.getMessage(), ie);
					} finally {
						_buf.clear();
					}
				}
				
				_rSelector.selectedKeys().clear();
			}
			
			// Check execution time
			long execTime = System.currentTimeMillis() - lastExecTime;
			if (execTime > 1250)
				log.warn("Excessive read time - " + execTime + "ms (" + consWaiting + " connections)");

			_status.complete();
		}
	}
}