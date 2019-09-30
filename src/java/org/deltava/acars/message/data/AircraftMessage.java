// Copyright 2006, 2010, 2018, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.beans.Pilot;
import org.deltava.beans.schedule.Aircraft;
import org.deltava.acars.message.DataRequest;
import org.deltava.acars.message.DataResponseMessage;

/**
 * An ACARS data response message to store aircraft information.
 * @author Luke
 * @version 8.7
 * @since 1.0
 */

public class AircraftMessage extends DataResponseMessage<Aircraft> {
	
	private boolean _showProfile;
	private boolean _showPolicy;

	/**
	 * Instantiates the Message.
	 * @param msgFrom the originating Pilot
	 * @param parentID the request message ID
	 */
	public AircraftMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, DataRequest.EQLIST, parentID);
	}
	
	/**
	 * Returns whether the entire fuel profile should be sent to the user, or just the aircraft name.
	 * @return TRUE if the profile should be sent, otherwise FALSE
	 */
	public boolean getShowProfile() {
		return _showProfile;
	}
	
	/**
	 * Returns whether all airline policies should be displayed.
	 * @return TRUE if all airline policies should be displayed, or FALSE for user's airline
	 */
	public boolean getShowPolicy() {
		return _showPolicy;
	}
	
	/**
	 * Updates whether the entire fuel profile should be sent to the user, or just the aircraft name.
	 * @param show TRUE if the profile should be sent, otherwise FALSE
	 */
	public void setShowProfile(boolean show) {
		_showProfile = show;
	}
	
	/**
	 * Updates whether all airline policy options should be returned.
	 * @param show TRUE if all policies should be returned, otherwise FALSE
	 */
	public void setShowPolicy(boolean show) {
		_showPolicy = show;
	}
}