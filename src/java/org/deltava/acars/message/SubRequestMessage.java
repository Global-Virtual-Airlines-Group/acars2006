// Copyright 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

/**
 * An interface for messages that have sub-requests. 
 * @author Luke
 * @version 9.1
 * @since 9.1
 */

public interface SubRequestMessage extends Message {

	/**
	 * Returns the sub-request.
	 * @return a SubRequest
	 */
	public SubRequest getRequestType();
}