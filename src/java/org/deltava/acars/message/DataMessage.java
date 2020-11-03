// Copyright 2005 2006, 2008, 2009, 2011, 2012, 2013, 2014, 2015, 2018, 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * An ACARS data request/response message.
 * @author Luke
 * @version 9.1
 * @since 1.0
 */

public abstract class DataMessage extends AbstractMessage implements SubRequestMessage {
	
	private final DataRequest _type;

	/**
	 * Creates the message.
	 * @param type the MessageType
	 * @param msgFrom the originating user
	 * @param reqType the DataRequest
	 */
	protected DataMessage(MessageType type, Pilot msgFrom, DataRequest reqType) {
		super(type, msgFrom);
		_type = reqType;
	}
	
	@Override
	public SubRequest getRequestType() {
		return _type;
	}
}