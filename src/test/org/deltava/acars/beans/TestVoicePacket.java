package org.deltava.acars.beans;

import java.io.*;

import org.deltava.beans.Pilot;

import org.deltava.acars.beans.Packet.PacketInputStream;
import org.deltava.acars.message.VoiceMessage;

import junit.framework.TestCase;

public class TestVoicePacket extends TestCase {
	
	public void testLoadPackets() throws IOException {
		
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
}