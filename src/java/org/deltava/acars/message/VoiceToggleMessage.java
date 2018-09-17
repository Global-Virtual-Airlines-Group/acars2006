// Copyright 2011, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * An ACARS message to toggle voice.
 * @author Luke
 * @version 8.4
 * @since 4.0
 */

public class VoiceToggleMessage extends AbstractMessage {
	
	private final boolean _enabled;
	private final boolean _echo;

	/**
	 * Creates the message.
	 * @param msgFrom the originating Pilot
	 * @param isEnabled TRUE if voice enabled, FALSE to disconnect
	 * @param isEcho TRUE if voice echo, otherwise FALSE
	 */
	public VoiceToggleMessage(Pilot msgFrom, boolean isEnabled, boolean isEcho) {
		super(MessageType.VOICETOGGLE, msgFrom);
		_enabled = isEnabled;
		_echo = isEcho;
	}

	/**
	 * Returns whether the User is requesting voice enabled.
	 * @return TRUE if voice enabled, FALSE to disconnect
	 */
	public boolean getVoiceEnabled() {
		return _enabled;
	}
	
	/**
	 * Returns whether the User is requesting voice ecjp.
	 * @return TRUE if voice echo, otherwise FALSE
	 */
	public boolean getVoiceEcho() {
		return _echo;
	}
}