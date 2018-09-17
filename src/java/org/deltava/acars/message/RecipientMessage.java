// Copyright 2004, 2009, 2011, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * An ACARS message with an optional recipeint.
 * @author Luke
 * @version 8.4
 * @since 2.8
 */

public abstract class RecipientMessage extends AbstractMessage {
	
	private String _recipient;

	/**
	 * Creates the message.
	 * @param type the MessageType
	 * @param msgFrom the originating Pilot
	 */
	protected RecipientMessage(MessageType type, Pilot msgFrom) {
		super(type, msgFrom);
	}
	
	/**
	 * Returns the recipient pilot code.
	 * @return the pilot code
	 */
	public final String getRecipient() {
		return _recipient;
	}
	
	/**
	 * Updates the recipient of this message.
	 * @param msgTo the recipient pilot code, or null if none
	 */
	public final void setRecipient(String msgTo) {
		_recipient = msgTo;
	}
}