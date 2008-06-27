// Copyright 2006, 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import org.deltava.acars.message.Message;

/**
 * An Envelope for Message beans.
 * @author Luke
 * @version 2.2
 * @since 1.0
 */

public class MessageEnvelope extends Envelope<Message> {

	/**
	 * Initializes the Envelope.
	 * @param msgData the Message
	 * @param conID the connection ID
	 */
	public MessageEnvelope(Message msgData, long conID) {
		super(msgData, msgData.getSender(), msgData.getTime(), conID);
	}
}