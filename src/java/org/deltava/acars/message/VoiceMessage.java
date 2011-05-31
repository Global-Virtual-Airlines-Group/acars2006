// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.*;
import org.deltava.beans.mvs.*;

/**
 * An ACARS Voice packet message.
 * @author Luke
 * @version 4.0
 * @since 4.0
 */

public class VoiceMessage extends AbstractMessage {
	
	private String _channel;
	private SampleRate _rate;
	private VoiceCompression _compression;
	private long _crc32;
	private GeoLocation _loc;
	
	private byte[] _data;

	/**
	 * Creates the message.
	 * @param msgFrom the originating Pilot
	 * @param channel the channel name
	 */
	public VoiceMessage(Pilot msgFrom, String channel) {
		super(Message.MSG_VOICE, msgFrom);
		_channel = channel;
	}
	
	/**
	 * Returns the channel name.
	 * @return the channel
	 */
	public String getChannel() {
		return _channel;
	}
	
	/**
	 * Returns the packet payload.
	 * @return the payload
	 */
	public byte[] getData() {
		return _data;
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
	 * Returns the CRC-32 checksum of the payload. For compressed packets, this is
	 * the checksum of the <i>compressed</i> payload.
	 * @return the CRC-32
	 */
	public long getCRC32() {
		return _crc32;
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
		_crc32 = crc32;
	}
	
	/**
	 * Updates the location of the sender.
	 * @param loc the sender location
	 */
	public void setLocation(GeoLocation loc) {
		_loc = loc;
	}
	
	/**
	 * Sets the payload for this packet.
	 * @param data the packet payload
	 */
	public void setData(byte[] data) {
		_data = data;
	}
}