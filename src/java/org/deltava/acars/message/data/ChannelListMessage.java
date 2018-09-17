// Copyright 2011, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import java.util.*;

import org.deltava.acars.message.*;

import org.deltava.beans.Pilot;
import org.deltava.beans.mvs.PopulatedChannel;

/**
 * A message to request voice channels.
 * @author Luke
 * @version 8.4
 * @since 4.0
 */

public class ChannelListMessage extends DataResponseMessage<PopulatedChannel> {
	
	private boolean _clearList = true;

	private final Map<Long, Integer> _warnings = new HashMap<Long, Integer>();
	
	/**
	 * Creates the message.
	 * @param msgFrom the originating user
	 * @param parentID the parent message ID
	 */
	public ChannelListMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, DataRequest.CHLIST, parentID);
	}

	/**
	 * Returns whether the client should clear its channel list upon receipt.
	 * @return TRUE if a complete list, otherwise FALSE
	 */
	public boolean getClearList() {
		return _clearList;
	}
	
	/**
	 * Returns the warning level for a particular connection.
	 * @param conID the Connection ID
	 * @return the warning level
	 */
	public int getWarning(Long conID) {
		return _warnings.getOrDefault(conID, Integer.valueOf(0)).intValue();
	}
	
	/**
	 * Sets whether the client should clear its channel list upon receipt.
	 * @param doClear TRUE if a complete list, otherwise FALSE
	 */
	public void setClearList(boolean doClear) {
		_clearList = doClear;
	}

	/**
	 * Sets the warning levels.
	 * @param warnLevels a Map of warning counts, keyed by Connection ID
	 */
	public void setWarnings(Map<Long, Integer> warnLevels) {
		_warnings.putAll(warnLevels);
	}
}