// Copyright 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.beans.Pilot;
import org.deltava.beans.schedule.Aircraft;

import org.deltava.acars.message.DataResponseMessage;

/**
 * An ACARS data response message to store aircraft information.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class AircraftMessage extends DataResponseMessage<Aircraft> {
	
	private boolean _showProfile;

	/**
	 * Instantiates the Message.
	 * @param msgFrom the originating Pilot
	 * @param parentID the request message ID
	 */
	public AircraftMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, REQ_EQLIST, parentID);
	}
	
	/**
	 * Returns wether the entire fuel profile should be sent to the user, or just the aircraft name.
	 * @return TRUE if the profile should be sent, otherwise FALSE
	 */
	public boolean getShowProfile() {
		return _showProfile;
	}
	
	/**
	 * Updates wether the entire fuel profile should be sent to the user, or just the aircraft name.
	 * @param show TRUE if the profile should be sent, otherwise FALSE
	 */
	public void setShowProfile(boolean show) {
		_showProfile = show;
	}
}