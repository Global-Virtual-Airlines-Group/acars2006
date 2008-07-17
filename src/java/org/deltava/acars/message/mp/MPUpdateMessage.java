// Copyright 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.mp;

import org.deltava.acars.beans.MPUpdate;

import org.deltava.acars.message.*;

/**
 * A Multi-Player message to handle batch updates of all aircraft positions.
 * @author Luke
 * @version 2.2
 * @since 2.2
 */

public class MPUpdateMessage extends DataResponseMessage<MPUpdate> {
	
	private boolean _showLivery;
	private boolean _doClear;

	/**
	 * Initializes the Message.
	 * @param doClear TRUE if the MP list should be cleared, otherwise FALSE
	 * @param parentID the ID of the InitMessage that created this message, or zero
	 */
	public MPUpdateMessage(boolean doClear, long parentID) {
		super(null, Message.MSG_MPUPDATE, parentID);
		_doClear = doClear;
	}
	
	/**
	 * Returns whether the MP list should be cleared.
	 * @return TRUE if positions should be cleared, otherwise FALSE
	 */
	public boolean isClear() {
		return _doClear;
	}
	
	/**
	 * Returns whether livery data is included in this message.
	 * @return TRUE if livery data included, otherwise FALSE
	 */
	public boolean hasLivery() {
		return _showLivery;
	}
	
	/**
	 * Sets whether livery data should be included.
	 * @param doShow TRUE if livery data should be included, otherwise FALSE
	 */
	public void setShowLivery(boolean doShow) {
		_showLivery = doShow;
	}
}