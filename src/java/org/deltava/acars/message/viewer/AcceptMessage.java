// Copyright 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.viewer;

import org.deltava.acars.message.*;

import org.deltava.beans.Pilot;

/**
 * An ACARS message to send Flight viewer requests. 
 * @author Luke
 * @version 2.8
 * @since 2.8
 */

public class AcceptMessage extends ViewerMessage {
	
	/**
	 * Creates the message.
	 * @param msgFrom the originating Instructor
	 */
	public AcceptMessage(Pilot msgFrom) {
		super(VIEW_ACCEPT, msgFrom);
	}
}