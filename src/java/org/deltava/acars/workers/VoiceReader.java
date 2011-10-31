// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.net.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;

import org.deltava.acars.ACARSException;
import org.deltava.acars.beans.*;
import org.deltava.acars.message.VoiceMessage;
import org.deltava.beans.mvs.PopulatedChannel;

import org.deltava.util.system.SystemData;

import org.gvagroup.ipc.WorkerStatus;

/**
 * An ACARS worker thread to read voice packets.
 * @author Luke
 * @version 4.1
 * @since 4.0
 */

public class VoiceReader extends Worker {
	
	private Selector _rSelector;
	private DatagramChannel _channel;
	
	private final ByteBuffer _buf = ByteBuffer.allocateDirect(32768);
	
	/**
	 * Creates the Worker.
	 */
	public VoiceReader() {
		super("Voice Reader", 100, VoiceReader.class);
	}
	
	/**
	 * Initializes the Worker.
	 */
	public final void open() {
		super.open();
		
		try {
			_channel = DatagramChannel.open();
			_channel.configureBlocking(false);
			
			// Bind to the port
			DatagramSocket socket = _channel.socket();
			socket.setReceiveBufferSize(SystemData.getInt("acars.buffer.recv"));
			socket.setSendBufferSize(SystemData.getInt("acars.buffer.send") * 4);
			socket.setReuseAddress(true);
			socket.bind(new InetSocketAddress(SystemData.getInt("acars.voice.port")));
			
			// Add the server socket channel to the selector
			_rSelector = Selector.open();
			_channel.register(_rSelector, SelectionKey.OP_READ);
		} catch (IOException ie) {
			log.error(ie.getMessage());
			throw new IllegalStateException(ie);
		}
	}
	
	/**
	 * Shuts down the Worker.
	 */
	public final void close() {
		try {
			_channel.socket().close();
			_rSelector.close();
		} catch (IOException ie) {
			log.error(ie.getMessage());	
		}
		
		super.close();
	}

	/**
	 * Executes the Thread.
	 */
	public void run() {
		log.info("Started");
		_status.setStatus(WorkerStatus.STATUS_START);
		long lastExecTime = 0;
		VoiceChannels vc = VoiceChannels.getInstance();
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
				try {
					InetSocketAddress srcAddr = (InetSocketAddress) _channel.receive(_buf);
					while (srcAddr != null) {
						String addr = srcAddr.toString().substring(1);
						log.info("Received voice packet from " + addr);
						
						// This might return null if it's the first UDP packet since we don't know what
						// source port it's coming from. Do a lookup based on the IP address only. 
						ACARSConnection ac = _pool.get(addr);
						if (ac == null) {
							ac = _pool.get(srcAddr.getAddress().getHostAddress());
							if (ac == null)
								throw new IllegalArgumentException(addr + " - not connected");
						} else if (!ac.isVoiceCapable())
							throw new IllegalArgumentException(ac.getUserID() + " is not voice enabled");

						// Register the source address
						ac.enableVoice(_channel, srcAddr);
						try {
							_pool.add(ac);
						} catch (ACARSException ae) {
							log.error("Cannot register source address - " + ae.getMessage());
						}
						
						// Get the data
						byte[] pktData = new byte[_buf.flip().limit()];
						_buf.get(pktData);
						
						// Log the read
						ac.logVoice(pktData.length);
						
						// If it's a ping (ie. a 16-byte datagram), send it right back, otherwise push onto the queue
						if (pktData.length == 16) {
							log.info("Received MVS Ping from " + ac.getUserID());
							BinaryEnvelope oenv = new BinaryEnvelope(ac.getUser(), pktData, ac.getID());
							RAW_OUTPUT.add(oenv);
						} else {
							// Get the channel
							PopulatedChannel pc = vc.get(ac.getID());
							if (pc == null)
								throw new IllegalArgumentException(ac.getUserID() + " (not in any channel)");
							
							// Create the message
							VoiceMessage msg = new VoiceMessage(ac.getUser(), pc.getChannel().getName());
							msg.setConnectionID(ac.getID());
							msg.setData(pktData);
							MSG_INPUT.add(new MessageEnvelope(msg, ac.getID()));
						}
						
						_buf.clear();
						srcAddr = (InetSocketAddress) _channel.receive(_buf);
					}
				} catch (IllegalArgumentException iae) {
					log.error("Unexpected voice packet from " + iae.getMessage());
					_buf.clear();
				} catch (IOException ie) {
					log.error("Error reading voice packet - " + ie.getMessage(), ie);
					_buf.clear();
				}
			}
			
			// Check execution time
			_rSelector.selectedKeys().clear();
			long execTime = System.currentTimeMillis() - lastExecTime;
			if (execTime > 1250)
				log.warn("Excessive read time - " + execTime + "ms (" + consWaiting + " connections)");

			// Log executiuon
			_status.complete();
		}
	}
}