// Copyright 2011, 2014, 2106 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.*;

/**
 * An ACARS Voice packet message.
 * @author Luke
 * @version 7.2
 * @since 4.0
 */

public class VoiceMessage extends AbstractMessage {
	
	private final byte[] _data;

	/**
	 * Creates the message.
	 * @param msgFrom the originating Pilot
	 * @param data the message data
	 */
	public VoiceMessage(Pilot msgFrom, byte[] data) {
		super(Message.MSG_VOICE, msgFrom);
		_data = data;
	}
	
	/**
	 * Returns the packet payload.
	 * @return the payload
	 */
	public byte[] getData() {
		return _data;
	}
}