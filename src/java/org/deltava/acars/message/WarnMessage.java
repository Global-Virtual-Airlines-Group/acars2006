// Copyright 2010, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * An ACARS Message to send a Warning to another user.
 * @author Luke
 * @version 4.0
 * @since 4.0
 */

public class WarnMessage extends RecipientMessage {
	
	private boolean _reset;
	
	/**
	 * Creates the Message.
	 * @param sender the message sender
	 * @param resetLevel TRUE if the level should be reset, otherwise FALSE
	 */
	public WarnMessage(Pilot sender, boolean resetLevel) {
		super(Message.MSG_WARN, sender);
		_reset = resetLevel;
	}
	
	/**
	 * Returns whether the warning level should be reset.
	 * @return TRUE if the level should be reset, otherwise FALSE
	 */
	public boolean isReset() {
		return _reset;
	}
}