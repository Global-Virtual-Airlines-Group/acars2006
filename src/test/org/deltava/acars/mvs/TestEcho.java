package org.deltava.acars.mvs;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Random;
import java.util.concurrent.*;
import java.util.zip.CRC32;

import org.apache.log4j.*;

import org.deltava.beans.mvs.*;
import org.deltava.beans.schedule.GeoPosition;
import org.deltava.util.*;

import junit.framework.TestCase;

public class TestEcho extends TestCase {
	
	protected Logger log;
	
	private static final Random _rnd = new Random();
	protected final BlockingQueue<Long> _ids = new LinkedBlockingQueue<Long>();
	
	private class UDPReader implements Runnable {
		
		private final DatagramChannel _dc;
		
		UDPReader(DatagramChannel dc) {
			super();
			_dc = dc;
		}
		
		@Override
		public void run() {
			try {
				Selector s = Selector.open();
				_dc.register(s, SelectionKey.OP_READ);
				
				s.select(); ByteBuffer buf = ByteBuffer.allocate(2048);
				while (!Thread.currentThread().isInterrupted()) {
					SocketAddress srcAddr = _dc.receive(buf);
					
					// Get the data
					byte[] pktData = new byte[buf.flip().limit()];
					buf.get(pktData);
					buf.clear();
					
					log.info("Received " + pktData.length + " bytes from " + NetworkUtils.getSourceAddress(srcAddr));
					Packet p = Packet.parse(pktData);
					
					boolean wasSent = _ids.remove(Long.valueOf(p.getID()));
					if (!wasSent)
						log.warn("Received unknown packet ID " + p.getID());
					else
						log.info("Received packet ID " + p.getID());
					
					s.select();
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			
			log.info("Reader shut down");
		}
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		// Init Log4j
		PropertyConfigurator.configure("etc/log4j.test.properties");
		log = Logger.getLogger(TestEcho.class);
	}

	@Override
	protected void tearDown() throws Exception {
		LogManager.shutdown();
		super.tearDown();
	}

	public void testPacketEcho() throws Exception {
		
		DatagramChannel dc = DatagramChannel.open();
		dc.configureBlocking(false);
		assertFalse(dc.isBlocking());
		dc.bind(new InetSocketAddress(15527));
		
		Selector s = Selector.open();
		dc.register(s, SelectionKey.OP_WRITE);
		
		ByteBuffer buf = ByteBuffer.allocate(2048);
		InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName("nephele.gvagroup.org"), 15527);
		assertFalse(dc.isConnected());
		
		// Start the listener
		Thread t = new Thread(new UDPReader(dc), "UDP Listener");
		t.setDaemon(true);
		t.start();
		
		for (int x = 1; x < 15; x++) {
			buf.put(buildPacket(x));
			_ids.add(Long.valueOf(x));
			buf.flip();
			
			while (buf.hasRemaining()) {
				if (s.select(200) > 0) {
					dc.send(buf, addr);
					s.selectedKeys().clear();
				}
			}
			
			buf.clear();
			log.info("Sent packet ID " + x);
			Thread.sleep(350);
		}
		
		// Wait for all packets
		int totalTime = 0;
		while ((totalTime < 5000) && !_ids.isEmpty()) {
			Thread.sleep(200);
			totalTime += 200;
		}
		
		ThreadUtils.kill(t, 5000);
	}
	
	private static byte[] buildPacket(long id) {
		
		// Create payload
		byte[] data = new byte[1280];
		_rnd.nextBytes(data);

		Packet p = new Packet();
		p.setUserID("test User");
		p.setCompression(VoiceCompression.NONE);
		p.setRate(SampleRate.SR6K);
		p.setID(id);
		p.setLocation(new GeoPosition(33, -85.3));
		p.setData(data);
		
		// Calculate CRC32
		CRC32 crc = new CRC32();
		crc.update(data);
		p.setCRC32(crc.getValue());
		
		return Packet.rewrite(p);
	}
}