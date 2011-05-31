// Copyright 2010, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import java.io.*;
import java.util.zip.CRC32;

import org.apache.log4j.Logger;

import org.deltava.acars.message.VoiceMessage;

import org.deltava.beans.mvs.*;
import org.deltava.beans.GeoLocation;
import org.deltava.beans.schedule.GeoPosition;

import org.deltava.util.StringUtils;

/**
 * A utility class to convert MVS packets to and from DataEnvelopes. 
 * @author Luke
 * @version 4.0
 * @since 1.0
 */

public class Packet {
	
	private static final String HDR = "MVX";
	private static final int PROTOCOL_VERSION = 1;
	
	private static final Logger log = Logger.getLogger(Packet.class);
	
	private static class PacketInputStream extends DataInputStream {
		
		PacketInputStream(InputStream is) {
			super(is);
		}
		
		public String readUTF8() throws IOException {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			int b = read();
			while (b > 0) {
				os.write(b);
				b = read();
			}
			
			return new String(os.toByteArray(), "UTF-8");
		}
	}
	
	private static class PacketOutputStream extends DataOutputStream {
		
		PacketOutputStream(OutputStream os) {
			super(os);
		}

		public void writeUTF8(String data) throws IOException {
			byte[] d = data.getBytes("UTF-8");
			write(d);
			write(0);
		}
	}
	
	// static class
	private Packet() {
		super();
	}

	/**
	 * Creates a DataEnvelope from an MVS voice packet.
	 * @param user the User
	 * @param pktData the packet data
	 * @return a DataEnvelope
	 * @throws IOException if an error occurs
	 */
	public static void parse(VoiceMessage msg) throws IOException {
		
		PacketInputStream in = new PacketInputStream(new ByteArrayInputStream(msg.getData()));
		String hdr = in.readUTF8();
		if ((hdr == null) || !hdr.startsWith(HDR))
			throw new IOException("Invalid Header - " + hdr);
		
		// Check the version
		int ver = StringUtils.parse(hdr.substring(HDR.length()), 0);
		if (ver != PROTOCOL_VERSION)
			throw new IOException("Unknown Protocol - " + hdr);
		
		// Check user
		String userID = in.readUTF8();
		if (!msg.getSenderID().equals(userID))
			throw new IOException("Invalid User ID - expected " + msg.getSenderID() + " was " + userID);
		
		// Check channel
		String channel = in.readUTF8();
		if (!msg.getChannel().equals(channel))
			throw new IOException("Invalid Channel - expected " + msg.getChannel() + " was " + channel);
		
		// Load data
		msg.setCompression(VoiceCompression.values()[in.readInt()]);
		msg.setID(in.readLong());
		msg.setRate(SampleRate.get(in.readInt()));
		
		// Load Location
		double lat = in.readDouble();
		double lng = in.readDouble();
		if ((lat != 0.0d) || (lng != 0.0d))
			msg.setLocation(new GeoPosition(lat, lng));
			
		// Load the data
		msg.setCRC32(in.readLong());
		int dataLength = in.readInt();
		byte[] data = new byte[dataLength];
		int actualLength = in.read(data);
        if (actualLength != dataLength)
            throw new IOException("Expected " + dataLength + " payload, received " + actualLength);
        
		// Get the CRC-32
		CRC32 crc = new CRC32();
		crc.update(data);
		if (crc.getValue() != msg.getCRC32())
			throw new IOException("Invalid CRC-32");
	}

	/**
	 * Converts a VoiceMessage into a data packet.
	 * @param env the DataEnvelope
	 * @return the packet data
	 */
	public static byte[] create(VoiceMessage msg) {
		
		StringBuilder hdr = new StringBuilder(HDR);
		hdr.append(PROTOCOL_VERSION);
		try {
			ByteArrayOutputStream pkt = new ByteArrayOutputStream(1024);
			PacketOutputStream os = new PacketOutputStream(pkt);
			os.writeUTF8(hdr.toString());
			os.writeUTF8(msg.getSenderID());
			os.writeUTF8(msg.getChannel());
			os.writeInt(msg.getCompression().getType());
			os.writeLong(msg.getID());
			os.writeInt(msg.getRate().getRate());
			if (msg.getLocation() != null) {
				GeoLocation loc = msg.getLocation();
				os.writeDouble(loc.getLatitude());
				os.writeDouble(loc.getLongitude());
			} else {
				os.writeDouble(0);
				os.writeDouble(0);
			}
			
			// Write the payload
			os.writeLong(msg.getCRC32());
			byte[] data = msg.getData();
			os.writeInt(data.length);
			os.write(data);
			return pkt.toByteArray();
		} catch (IOException ie) {
			log.error(ie.getMessage(), ie);
		}

		return null;
	}
}