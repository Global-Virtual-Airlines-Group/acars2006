// Copyright 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.mp;

import org.deltava.acars.message.*;

import org.deltava.beans.Pilot;

/**
 * A message to track multiplayer data.
 * @author Luke
 * @version 2.2
 * @since 2.2
 */

public class MPMessage extends LocationMessage {
	
	private String _acCode;
	
	/**
	 * Initializes the message.
	 * @param msgFrom the originating Pilot
	 */
	public MPMessage(Pilot msgFrom) {
		super(Message.MSG_MPUPDATE, msgFrom);
	}
	
	/**
	 * Returns the aircraft code, used for model matching.
	 * @return the aircraft code
	 */
	public String getAircraftCode() {
		return _acCode;
	}
	
	/**
	 * Updates the aircraft code for this connection.
	 * @param code the aircraft code
	 */
	public void setAircraftCode(String code) {
		_acCode = code;
	}
}