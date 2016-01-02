package org.deltava.acars.mvs;

import java.io.*;
import java.util.Random;
import java.util.zip.CRC32;

import org.deltava.beans.mvs.*;
import org.deltava.beans.schedule.GeoPosition;
import org.deltava.acars.util.PacketInputStream;

import junit.framework.TestCase;

public class TestVoicePacket extends TestCase {
	
	private final Random _rnd = new Random();
	
	public void testCSPackets() throws IOException {
		
		File f = new File("c:\\temp\\test.mvs");
		assertTrue(f.exists());
		assertNotNull(_rnd);
		
		// Start loading stuff
		PacketInputStream ds = new PacketInputStream(new FileInputStream(f));
		while (ds.available() > 0) {
			int dataLength = ds.readInt32();
			assertTrue(dataLength >= 0);
			byte[] pktData = new byte[dataLength];
			ds.read(pktData);
			
			Packet pkt = Packet.parse(pktData);
			assertNotNull(pkt);
		}
		
		ds.close();
	}
	
	public void testJavaPackets() throws IOException {
		
		byte[] data = new byte[1280];
		_rnd.nextBytes(data);
		
		Packet pkt = new Packet();
		pkt.setUserID("foo");
		pkt.setCompression(VoiceCompression.NONE);
		pkt.setRate(SampleRate.SR6K);
		pkt.setID(1);
		pkt.setLocation(new GeoPosition(33, -85.3));
		pkt.setData(data);
		
		// Calculate CRC32
		CRC32 crc = new CRC32();
		crc.update(data);
		pkt.setCRC32(crc.getValue());
		
		// Now read the packet back
		byte[] pd = Packet.rewrite(pkt);
		Packet p2 = Packet.parse(pd);
		assertEquals(2, Packet.getVersion(pd));
		
		// Compare
		assertEquals(pkt.getUserID(), p2.getUserID());
		assertEquals(pkt.getCompression(), p2.getCompression());
		assertEquals(pkt.getRate(), p2.getRate());
		assertEquals(pkt.getID(), p2.getID());
		assertEquals(pkt.getConnectionID(), p2.getConnectionID());
		
		// Now read the packet back
		p2.setConnectionID(1234);
		byte[] pd2 = Packet.rewrite(p2);
		Packet p3 = Packet.parse(pd2);
		assertEquals(2, Packet.getVersion(pd2));
		assertEquals(p2.getConnectionID(), p3.getConnectionID());
		
		// Test packet version of junk data
		assertEquals(-1, Packet.getVersion(data));
	}
}