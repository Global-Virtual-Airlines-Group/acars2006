// Copyright 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.beans.Pilot;
import org.deltava.beans.navdata.NavigationDataBean;

import org.deltava.acars.message.DataResponseMessage;

/**
 * An ACARS data response message to store navigation data information. 
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class NavigationDataMessage extends DataResponseMessage<NavigationDataBean> {

	/**
	 * Instantiates the Message.
	 * @param msgFrom the originating Pilot
	 * @param parentID the request message ID
	 */
	public NavigationDataMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, REQ_NAVAIDINFO, parentID);
	}
}