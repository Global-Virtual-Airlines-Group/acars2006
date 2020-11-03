// Copyright 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

/**
 * An interface to describe ACARRS subsrequest types.
 * @author Luke
 * @version 9.1
 * @since 9.1
 */

public interface SubRequest {

	/**
	 * Returns the subrequest code.
	 * @return the code
	 */
	public String getCode();
	
	/**
	 * Returns the subrequest type.
	 * @return a SubRequestType
	 */
	public SubRequestType getType();
}