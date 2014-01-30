// Copyright 2010, 2011, 2012, 2014 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.mvs;

import java.io.*;
import java.util.zip.CRC32;

import org.deltava.beans.GeoLocation;
import org.deltava.beans.mvs.*;
import org.deltava.beans.schedule.GeoPosition;
import org.deltava.util.*;

/**
 * A utility class to parse MVS packets.
 * @author Luke
 * @version 5.2
 * @since 1.0
 */

public class Packet {

	private static final String HDR = "MVX";
	public static final int PROTOCOL_VERSION = 2;
	
	private SampleRate _rate;
	private VoiceCompression _compression;
	private long _crc32;
	private GeoLocation _loc;
	private long _id;
	private long _conID;
	
	private byte[] _data;
	
	// static/test methods construct only
	Packet() {
		super();
	}
	
	/**
	 * Returns the compression flags.
	 * @return the compression
	 */
	public VoiceCompression getCompression() {
		return _compression;
	}
	
	/**
	 * Returns the data sampling rate.
	 * @return the rate
	 */
	public SampleRate getRate() {
		return _rate;
	}

	/**
	 * Returns the Location of the sender.
	 * @return the Location
	 */
	public GeoLocation getLocation() {
		return _loc;
	}
	
	/**
	 * Returns the sequence ID.
	 * @return the sequence ID
	 */
	public long getID() {
		return _id;
	}
	
	/**
	 * Returns the connection ID.
	 * @return the connection ID
	 */
	public long getConnectionID() {
		return _conID;
	}
	
	/**
	 * Returns the CRC-32 checksum of the payload. For compressed packets, this is
	 * the checksum of the <i>compressed</i> payload.
	 * @return the CRC-32
	 */
	public long getCRC32() {
		return _crc32;
	}
	
	/**
	 * Returns the packet payload.
	 * @return the payload
	 */
	public byte[] getData() {
		return _data;
	}
	
	/**
	 * Updates the data sampling rate of this packet.
	 * @param rt the rate
	 */
	public void setRate(SampleRate rt) {
		_rate = rt;
	}

	/**
	 * Updates the compression flags.
	 * @param vc the compression flags
	 */
	public void setCompression(VoiceCompression vc) {
		_compression = vc;
	}
	
	/**
	 * Updates the CRC-32 checksum of the payload.
	 * @param crc32 the CRC-32
	 */
	public void setCRC32(long crc32) {
		_crc32 = (crc32 & 0xFFFFFFFFL);
	}
	
	/**
	 * Updates the location of the sender.
	 * @param loc the sender location
	 */
	public void setLocation(GeoLocation loc) {
		_loc = loc;
	}
	
	/**
	 * Updates the sequence ID.
	 * @param id the sequence ID
	 */
	public void setID(long id) {
		_id = id;
	}
	
	/**
	 * Updates the connection ID.
	 * @param id the connection ID
	 */
	public void setConnectionID(long id) {
		_conID = id;
	}
	
	/**
	 * Updates the encoded voice data.
	 * @param data the data
	 */
	public void setData(byte[] data) {
		_data = data;
	}

	/**
	 * Parses an MVS voice packet.
	 * @param ac the ACARSConnection
	 * @param pkt the packet data
	 * @return msg the VoiceMessage
	 * @throws IOException if an error occurs
	 */
	public static Packet parse(byte[] pkt) throws IOException {
		Packet p = new Packet();
		try (PacketInputStream in = new PacketInputStream(new ByteArrayInputStream(pkt))) {
			String hdr = in.readUTF8();
			if ((hdr == null) || !hdr.startsWith(HDR)) throw new IOException("Invalid Header - " + hdr);

			// Check the version
			int ver = StringUtils.parse(hdr.substring(HDR.length()), 0);
			if (ver != PROTOCOL_VERSION) throw new IOException("Unknown Protocol - " + hdr);

			// Load data
			int flags = in.readInt32();
			p.setCompression(VoiceCompression.values()[flags & 0xf]);
			p.setConnectionID(in.readInt64());
			p.setID(in.readInt64());
			p.setRate(SampleRate.getRate(in.readInt32()));

			// Load Location
			GeoLocation pos = new GeoPosition(in.readDouble64(), in.readDouble64());
			if (GeoUtils.isValid(pos)) p.setLocation(pos);

			// Load the data
			p.setCRC32(in.readInt64());
			int dataLength = in.readInt32();
			byte[] data = new byte[dataLength];
			int actualLength = in.read(data);
			if (actualLength != dataLength) throw new IOException("Expected " + dataLength + " payload, received " + actualLength);
			p.setData(data);

			// Get the CRC-32
			CRC32 crc = new CRC32();
			crc.update(data);
			if (crc.getValue() != p.getCRC32())
				throw new IOException("Expected CRC " + Long.toHexString(p.getCRC32()) + ", received " + Long.toHexString(crc.getValue()));
			
			return p;
		}
	}

	/**
	 * Creates a packet.
	 * @param o the Packet
	 * @return the packet data
	 */
	public static byte[] rewrite(Packet p) {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream(512)) {
			try (PacketOutputStream out = new PacketOutputStream(bos)) {
				out.write(HDR + String.valueOf(PROTOCOL_VERSION));
				out.writeInt32(p._compression.ordinal());
				out.writeInt64(p._conID);
				out.writeInt64(p._id);
				out.writeInt32(p._rate.getRate());

				// Write location
				GeoLocation loc = p._loc;
				out.writeDouble64((loc == null) ? 0 : loc.getLatitude());
				out.writeDouble64((loc == null) ? 0 : loc.getLongitude());

				// Write the CRC
				out.writeInt64(p._crc32);

				// Write the data
				byte[] data = p._data;
				out.writeInt32(data.length);
				out.write(data);
			}
			
			return bos.toByteArray();
		} catch (IOException ie) {
			return null;
		}
	}
}