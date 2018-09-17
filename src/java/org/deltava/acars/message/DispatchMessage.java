// Copyright 2006, 2007, 2008, 2009, 2010, 2011, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * An ACARS message to store Dispatch information.
 * @author Luke
 * @version 8.4
 * @since 1.0
 */

public abstract class DispatchMessage extends RecipientMessage {
	
	private final DispatchRequest _reqType;
	
	/**
	 * Creates the message.
	 * @param dspType the DispatchType
	 * @param msgFrom the originating user
	 */
	protected DispatchMessage(DispatchRequest dspType, Pilot msgFrom) {
		super(MessageType.DISPATCH, msgFrom);
		_reqType = dspType;
	}
	
	/**
	 * Returns the Dispatch request type.
	 * @return the DispatchType
	 */
	public DispatchRequest getRequestType() {
		return _reqType;
	}
}