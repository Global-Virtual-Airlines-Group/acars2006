// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.net.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.VoiceMessage;
import org.deltava.beans.mvs.PopulatedChannel;

import org.deltava.util.system.SystemData;

import org.gvagroup.ipc.WorkerStatus;

/**
 * An ACARS worker thread to read voice packets.
 * @author Luke
 * @version 4.0
 * @since 4.0
 */

public class VoiceReader extends Worker {
	
	private Selector _rSelector;
	private DatagramChannel _channel;
	
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
			_rSelector = Selector.open();
			_channel = DatagramChannel.open();
			_channel.configureBlocking(false);
			
			// Bind to the port
			DatagramSocket socket = _channel.socket();
			SocketAddress sAddr = new InetSocketAddress(SystemData.getInt("acars.voice.port"));
			socket.setReceiveBufferSize(SystemData.getInt("acars.buffer.recv") * 2);
			socket.bind(sAddr);
			
			// Add the server socket channel to the selector
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

		final ByteBuffer buf = ByteBuffer.allocate(32768);
		while (!Thread.currentThread().isInterrupted()) {
			_status.setMessage("Listening for Voice packet");
			_status.execute();
			int consWaiting = 0;
			try {
				long runInterval = System.currentTimeMillis() - lastExecTime;
				if (runInterval < 50)
					Thread.sleep(50 - runInterval);
				
				consWaiting = _rSelector.select(SystemData.getInt("acars.sleep", 30000));
			} catch (InterruptedException ie) {
				log.warn("Interrupted");
				Thread.currentThread().interrupt();
			} catch (IOException ie) {
				log.warn("Error on select - " + ie.getMessage());
			}
			
			lastExecTime = System.currentTimeMillis();
			
			// See if we have some data
			if (consWaiting > 0) {
				_status.setMessage("Reading Inbound Voice Data");
				try {
					InetSocketAddress srcAddr = (InetSocketAddress) _channel.receive(buf);
					while (srcAddr != null) {
						ACARSConnection ac = _pool.get(srcAddr.getAddress().getHostAddress());
						if (ac == null)
							throw new IllegalArgumentException(srcAddr.getAddress().getHostAddress() + " - not connected");
						else if (!ac.isVoiceEnabled())
							throw new IllegalArgumentException(srcAddr.getAddress().getHostAddress() + " - not enabled");
						
						// Get the channel
						PopulatedChannel pc = VoiceChannels.get(ac.getID());
						if (pc == null)
							throw new IllegalArgumentException(ac.getUserID() + " not in any channel");
						
						// Create the message
						VoiceMessage msg = new VoiceMessage(ac.getUser(), pc.getChannel().getName());
						byte[] pktData = new byte[buf.flip().limit()];
						buf.get(pktData);
						msg.setData(pktData);
					
						// Create the envelope and push it onto the queue
						MSG_INPUT.add(new MessageEnvelope(msg, ac.getID()));
						buf.clear();
						srcAddr = (InetSocketAddress) _channel.receive(buf);
					}
				} catch (IllegalArgumentException iae) {
					log.warn("Unexpected voice packet from " + iae.getMessage());
					buf.clear();
				} catch (IOException ie) {
					log.error("Error reading voice packet - " + ie.getMessage(), ie);
				}
			}
			
			// Check execution time
			_rSelector.selectedKeys().clear();
			long execTime = System.currentTimeMillis() - lastExecTime;
			if (execTime > 2500)
				log.warn("Excessive read time - " + execTime + "ms (" + consWaiting + " connections)");

			// Log executiuon
			_status.complete();
		}
	}
}