// Copyright 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import org.deltava.acars.message.Message;

/**
 * An Envelope for Message beans.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class MessageEnvelope extends Envelope {

	/**
	 * @param msgData
	 * @param conID
	 */
	public MessageEnvelope(Message msgData, long conID) {
		super(msgData, conID);
	}

	public Message getMessage() {
		return (Message) _payload;
	}
}