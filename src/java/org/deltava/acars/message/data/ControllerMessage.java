// Copyright 2006, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.beans.*;
import org.deltava.beans.servinfo.Controller;

import org.deltava.acars.message.DataResponseMessage;

/**
 * An ACARS data response message to store ATC data.
 * @author Luke
 * @version 3.6
 * @since 1.0
 */

public class ControllerMessage extends DataResponseMessage<Controller> {
	
	private OnlineNetwork _network;

	/**
	 * Instantiates the Message.
	 * @param msgFrom the originating Pilot
	 * @param parentID the request message ID
	 */
	public ControllerMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, REQ_ATCINFO, parentID);
	}
	
	/**
	 * Returns the network for this ATC list.
	 * @return an OnlineNetwork
	 */
	public OnlineNetwork getNetwork() {
		return _network;
	}

	/**
	 * Sets the network for this ATC list.
	 * @param network an OnlineNetwork
	 */
	public void setNetwork(OnlineNetwork network) {
		_network = network;
	}
}