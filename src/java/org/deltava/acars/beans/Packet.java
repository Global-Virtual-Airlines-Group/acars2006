// Copyright 2010, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import java.io.*;
import java.util.zip.CRC32;

import org.deltava.acars.message.VoiceMessage;

import org.deltava.beans.GeoLocation;
import org.deltava.beans.mvs.*;
import org.deltava.beans.schedule.GeoPosition;

import org.deltava.util.StringUtils;

/**
 * A utility class to parse MVS packets. 
 * @author Luke
 * @version 4.0
 * @since 1.0
 */

public class Packet {
	
	private static final String HDR = "MVX";
	public static final int PROTOCOL_VERSION = 1;
	
	static class PacketInputStream extends DataInputStream {
		
		PacketInputStream(InputStream is) {
			super(is);
		}
		
		public final String readUTF8() throws IOException {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			int b = read();
			while (b > 0) {
				os.write(b);
				b = in.read();
			}
			
			return new String(os.toByteArray(), "UTF-8");
		}
		
		public final int readInt32() throws IOException {
	        int ch1 = in.read();
	        int ch2 = in.read();
	        int ch3 = in.read();
	        int ch4 = in.read();
	        if ((ch1 | ch2 | ch3 | ch4) < 0)
	            throw new EOFException();
	        
	        return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + ch1);
		}
		
		public final long readInt64() throws IOException {
			byte readBuffer[] = new byte[8];
	        readFully(readBuffer, 0, 8);
	        return (((long)readBuffer[7] << 56) +
	                ((long)(readBuffer[6] & 255) << 48) +
			((long)(readBuffer[5] & 255) << 40) +
	                ((long)(readBuffer[4] & 255) << 32) +
	                ((long)(readBuffer[3] & 255) << 24) +
	                ((readBuffer[2] & 255) << 16) +
	                ((readBuffer[1] & 255) <<  8) +
	                (readBuffer[0] & 255));
		}
		
		public final double readDouble64() throws IOException {
			return Double.longBitsToDouble(readInt64());
		}
	}
	
	static class PacketOutputStream extends DataOutputStream {
		
		PacketOutputStream(OutputStream os) {
			super(os);
		}
		
		public final void write(String s) throws IOException {
			write(s.getBytes("UTF-8"));
			write(0);
		}
		
		public final void writeInt32(int i) throws IOException {
			write (i & 0xFF);
			write((i >> 8) & 0xFF);
			write((i >> 16) & 0xFF);
			write((i >> 24) & 0xFF);
		}
		
		public final void writeInt64(long l) throws IOException {
			byte[] buffer = new byte[8];
			for (int x = 0; x < 8; x++)
				buffer[x] = (byte)((l >> (x * 8)) & 0xFF);
			
			write(buffer, 0, 8);
		}
		
		public final void writeDouble64(double d) throws IOException {
			writeInt64(Double.doubleToLongBits(d));
		}
	}
	
	// static class
	private Packet() {
		super();
	}

	/**
	 * Populates a VoiceMessage from an MVS voice packet. The packet is contained within
	 * the message's data.
	 * @param msg the VoiceMessage
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
		int flags = in.readInt32();
		msg.setCompression(VoiceCompression.values()[flags & 0xf]);
		msg.setConnectionID(in.readInt64()); // ignored, generally
		msg.setID(in.readInt64());
		msg.setRate(SampleRate.getRate(in.readInt32()));
		
		// Load Location
		double lat = in.readDouble64();
		double lng = in.readDouble64();
		if ((lat != 0.0d) || (lng != 0.0d))
			msg.setLocation(new GeoPosition(lat, lng));
			
		// Load the data
		msg.setCRC32(in.readInt64());
		int dataLength = in.readInt32();
		byte[] data = new byte[dataLength];
		int actualLength = in.read(data);
        if (actualLength != dataLength)
            throw new IOException("Expected " + dataLength + " payload, received " + actualLength);
        
        // Set the header size
        msg.setHeaderSize(msg.getData().length - dataLength);
        
		// Get the CRC-32
		CRC32 crc = new CRC32();
		crc.update(data);
		if (crc.getValue() != msg.getCRC32())
			throw new IOException("Invalid CRC-32! Expected " + Long.toHexString(msg.getCRC32()) + ", received "
					+ Long.toHexString(crc.getValue()));
	}

	/**
	 * Recreates a packet from a VoiceMessage.
	 * @param vmsg the VoiceMessage
	 * @return the packet data
	 * @throws IOException if an I/O error occurs
	 */
	public static byte[] rewrite(VoiceMessage vmsg) throws IOException {
		
		// If we don't have a header size, abort
		int hdrSize = vmsg.getHeaderSize();
		if (hdrSize < 1)
			throw new IllegalStateException("MVS Header size unknown");
		
		// Create flags
		int flags = vmsg.getCompression().ordinal();
		
		// Write the packet
		ByteArrayOutputStream bos = new ByteArrayOutputStream(2048);
		PacketOutputStream out = new PacketOutputStream(bos);
		out.write(HDR + String.valueOf(PROTOCOL_VERSION));
		out.write(vmsg.getSenderID());
		out.write(vmsg.getChannel());
		out.writeInt32(flags);
		out.writeInt64(vmsg.getConnectionID());
		out.writeInt64(vmsg.getID());
		out.writeInt32(vmsg.getRate().getRate());
		
		// Write location
		GeoLocation loc = (vmsg.getLocation() == null) ? new GeoPosition(0, 0) : vmsg.getLocation();
		out.writeDouble64(loc.getLatitude());
		out.writeDouble64(loc.getLongitude());
		
		// Write the CRC
		out.writeInt64(vmsg.getCRC32());
		
		// Write the data, but remember that vmsg.getData() is the entire packet, so strip off the header
		byte[] data = vmsg.getData();
		int dataLength = data.length - hdrSize;
		out.writeInt32(dataLength);
		out.write(data, hdrSize, dataLength);
		
		// Return the packet
		out.flush();
		return bos.toByteArray();
	}
}