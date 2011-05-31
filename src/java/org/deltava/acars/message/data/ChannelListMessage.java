// Copyright 2010 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.acars.message.*;

import org.deltava.beans.Pilot;
import org.deltava.beans.mvs.Channel;

/**
 * A message to request voice channels.
 * @author Luke
 * @version 4.0
 * @since 4.0
 */

public class ChannelListMessage extends DataResponseMessage<Channel> {

	/**
	 * Creates the message.
	 * @param msgFrom the originating user
	 * @param parentID the parent message ID
	 */
	public ChannelListMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, DataMessage.REQ_CHLIST, parentID);
	}
}