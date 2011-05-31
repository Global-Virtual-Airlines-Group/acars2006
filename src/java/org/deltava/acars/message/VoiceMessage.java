// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * An ACARS Voice packet message.
 * @author Luke
 * @version 4.0
 * @since 4.0
 */

public class VoiceMessage extends AbstractMessage {
	
	private byte[] _data;

	/**
	 * Creates the message.
	 * @param msgFrom the originating Pilot
	 */
	public VoiceMessage(Pilot msgFrom) {
		super(Message.MSG_VOICE, msgFrom);
	}
	
	/**
	 * Returns the packet payload.
	 * @return the payload
	 */
	public byte[] getData() {
		return _data;
	}
	
	/**
	 * Sets the payload for this packet.
	 * @param data the packet payload
	 */
	public void setData(byte[] data) {
		_data = data;
	}
}