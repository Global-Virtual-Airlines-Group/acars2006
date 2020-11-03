// Copyright 2006, 2007, 2008, 2009, 2010, 2011, 2018, 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * An ACARS message to store Dispatch information.
 * @author Luke
 * @version 9.1
 * @since 1.0
 */

public abstract class DispatchMessage extends RecipientMessage implements SubRequestMessage {
	
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

	@Override
	public SubRequest getRequestType() {
		return _reqType;
	}
}