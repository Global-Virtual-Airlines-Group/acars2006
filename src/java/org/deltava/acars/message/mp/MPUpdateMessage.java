// Copyright 2008, 2009, 2010, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.mp;

import java.util.*;

import org.deltava.acars.beans.MPUpdate;

import org.deltava.acars.message.*;

/**
 * A Multi-Player message to handle batch updates of all aircraft positions.
 * @author Luke
 * @version 7.2
 * @since 2.2
 */

public class MPUpdateMessage extends AbstractMessage {
	
	private final Collection<MPUpdate> _upds = new LinkedHashSet<MPUpdate>();
	private boolean _showLivery;
	private boolean _doClear;

	/**
	 * Initializes the Message.
	 * @param doClear TRUE if the MP list should be cleared, otherwise FALSE
	 */
	public MPUpdateMessage(boolean doClear) {
		super(Message.MSG_MPUPDATE, null);
		setProtocolVersion(2);
		_doClear = doClear;
	}
	
	/**
	 * Returns the multi-player updates in this Message.
	 * @return a Collection of MPUpdate beans
	 */
	public Collection<MPUpdate> getUpdates() {
		return _upds;
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
	 * Adds a multi-player update to the Message.
	 * @param upd the MPUpdate bean
	 * @return TRUE if the update was added, FALSE if already exists
	 */
	public boolean add(MPUpdate upd) {
		return _upds.add(upd);
	}
	
	/**
	 * Sets whether livery data should be included.
	 * @param doShow TRUE if livery data should be included, otherwise FALSE
	 */
	public void setShowLivery(boolean doShow) {
		_showLivery = doShow;
	}
}