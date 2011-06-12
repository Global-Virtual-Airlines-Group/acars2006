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

	private String _name;
	private String _desc;
	private String _freq;
	
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
	
	/**
	 * Returns the channel description.
	 * @return the description
	 */
	public String getDescription() {
		return _desc;
	}
	
	/**
	 * Returns the channel frequency.
	 * @return the frequency
	 */
	public String getFrequency() {
		return _freq;
	}
	
	/**
	 * Sets the channel description if creating a new channel.
	 * @param desc the description
	 */
	public void setDescription(String desc) {
		_desc = desc;
	}

	/**
	 * Sets the channel frequency if creating a new channel.
	 * @param desc the frequency
	 */
	public void setFrequency(String freq) {
		_freq = freq;
	}
}