// Copyright 2005 2006, 2008, 2009, 2011, 2012, 2013, 2014, 2015, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * An ACARS data request/response message.
 * @author Luke
 * @version 8.4
 * @since 1.0
 */

public abstract class DataMessage extends AbstractMessage {
	
	private DataRequest _type = DataRequest.UNKNOWN;

	/**
	 * Creates the message.
	 * @param type the MessageType
	 * @param msgFrom the originating user
	 */
	protected DataMessage(MessageType type, Pilot msgFrom) {
		super(type, msgFrom);
	}
	
	/**
	 * Returns the request type.
	 * @return the DataRequest
	 * @see DataMessage#setRequestType(DataRequest)
	 */
	public DataRequest getRequestType() {
		return _type;
	}

	/**
	 * Sets the request/response type.
	 * @param rt the DataReuqest type
	 */
	public void setRequestType(DataRequest rt) {
		_type = rt;
	}
}