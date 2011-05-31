// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * An ACARS message to toggle voice.
 * @author Luke
 * @version 4.0
 * @since 4.0
 */

public class VoiceToggleMessage extends AbstractMessage {
	
	private boolean _enabled;

	/**
	 * Creates the message.
	 * @param msgFrom the originating Pilot
	 * @param isEnabled TRUE if voice enabled, FALSE to disconnect
	 */
	public VoiceToggleMessage(Pilot msgFrom, boolean isEnabled) {
		super(Message.MSG_VOICETOGGLE, msgFrom);
		_enabled = isEnabled;
	}

	/**
	 * Returns whether the User is requesting voice enabled.
	 * @return TRUE if voice enabled, FALSE to disconnect
	 */
	public boolean getVoiceEnabled() {
		return _enabled;
	}
}