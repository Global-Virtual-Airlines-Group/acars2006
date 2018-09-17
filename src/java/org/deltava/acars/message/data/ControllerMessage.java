// Copyright 2006, 2011, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.beans.*;
import org.deltava.beans.servinfo.Controller;

import org.deltava.acars.message.*;

/**
 * An ACARS data response message to store ATC data.
 * @author Luke
 * @version 8.4
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
		super(msgFrom, DataRequest.ATCINFO, parentID);
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