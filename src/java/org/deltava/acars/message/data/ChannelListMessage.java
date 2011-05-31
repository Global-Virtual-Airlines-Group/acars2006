// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.acars.message.*;

import org.deltava.beans.Pilot;
import org.deltava.beans.mvs.PopulatedChannel;

/**
 * A message to request voice channels.
 * @author Luke
 * @version 4.0
 * @since 4.0
 */

public class ChannelListMessage extends DataResponseMessage<PopulatedChannel> {
	
	private boolean _clearList = true;

	/**
	 * Creates the message.
	 * @param msgFrom the originating user
	 * @param parentID the parent message ID
	 */
	public ChannelListMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, DataMessage.REQ_CHLIST, parentID);
	}

	/**
	 * Returns whether the client should clear its channel list upon receipt.
	 * @return TRUE if a complete list, otherwise FALSE
	 */
	public boolean getClearList() {
		return _clearList;
	}
	
	/**
	 * Sets whether the client should clear its channel list upon receipt.
	 * @param doClear TRUE if a complete list, otherwise FALSE
	 */
	public void setClearList(boolean doClear) {
		_clearList = doClear;
	}
}