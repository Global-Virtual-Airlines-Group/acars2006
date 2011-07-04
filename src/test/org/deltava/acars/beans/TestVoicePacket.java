package org.deltava.acars.beans;

import java.io.*;
import java.util.Random;
import java.util.zip.CRC32;

import org.deltava.beans.Pilot;
import org.deltava.beans.mvs.*;
import org.deltava.beans.schedule.GeoPosition;

import org.deltava.acars.beans.Packet.*;
import org.deltava.acars.message.VoiceMessage;

import junit.framework.TestCase;

public class TestVoicePacket extends TestCase {
	
	private final Random _rnd = new Random();
	
	public void testCSPackets() throws IOException {
		
		File f = new File("c:\\temp\\test.mvs");
		assertTrue(f.exists());
		
		// Start loading stuff
		PacketInputStream ds = new Packet.PacketInputStream(new FileInputStream(f));
		while (ds.available() > 0) {
			int dataLength = ds.readInt32();
			assertTrue(dataLength >= 0);
			byte[] pktData = new byte[dataLength];
			ds.read(pktData);
			
			// Create a voice message and convert
			Pilot p = new Pilot("Luke", "Kollin");
			p.setPilotCode("DVA043");
			VoiceMessage vmsg = new VoiceMessage(p, "Lobby");
			vmsg.setData(pktData);
			Packet.parse(vmsg);
		}
		
		ds.close();
	}
	
	public void testJavaPackets() throws IOException {
		
		Pilot p = new Pilot("Luke", "Kollin");
		p.setPilotCode("DVA043");
		VoiceMessage vmsg = new VoiceMessage(p, "Lobby");
		vmsg.setCompression(VoiceCompression.NONE);
		vmsg.setRate(SampleRate.SR11K);
		vmsg.setID(1);
		vmsg.setLocation(new GeoPosition(33, -85.3));
		
		// Create payload
		byte[] data = new byte[1280];
		_rnd.nextBytes(data);
		vmsg.setData(data);
		
		// Calculate CRC32
		CRC32 crc = new CRC32();
		crc.update(data);
		vmsg.setCRC32(crc.getValue());
		
		// Now read the packet back
		VoiceMessage vmsg2 = new VoiceMessage(vmsg.getSender(), vmsg.getChannel());
		vmsg2.setData(Packet.rewrite(vmsg));
		Packet.parse(vmsg2);
		
		// Compare
		assertEquals(vmsg.getCompression(), vmsg2.getCompression());
		assertEquals(vmsg.getRate(), vmsg2.getRate());
		assertEquals(vmsg.getID(), vmsg2.getID());
		assertEquals(vmsg.getConnectionID(), vmsg2.getConnectionID());
		
		// Now read the packet back
		vmsg.setConnectionID(1234);
		VoiceMessage vmsg3 = new VoiceMessage(vmsg.getSender(), vmsg.getChannel());
		vmsg3.setData(Packet.rewrite(vmsg));
		Packet.parse(vmsg3);
		assertEquals(vmsg.getConnectionID(), vmsg3.getConnectionID());
	}
}