// Copyright 2004, 2009, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * An ACARS message with an optional recipeint.
 * @author Luke
 * @version 4.0
 * @since 2.8
 */

public abstract class RecipientMessage extends AbstractMessage {
	
	private String _recipient;

	protected RecipientMessage(int type, Pilot msgFrom) {
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