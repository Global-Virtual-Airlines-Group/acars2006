// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * An ACARS message to switch voice channels.
 * @author Luke
 * @version 4.0
 * @since 4.0
 */

public class SwitchChannelMessage extends AbstractMessage {

	public String _name;
	
	/**
	 * Creates the message.
	 * @param msgFrom the originating pilot.
	 * @param channel the channel name to switch to
	 */
	public SwitchChannelMessage(Pilot msgFrom, String channel) {
		super(Message.MSG_SWCHAN, msgFrom);
		_name = channel;
	}

	/**
	 * Returns the channel name.
	 * @return the name
	 */
	public String getChannel() {
		return _name;
	}
}