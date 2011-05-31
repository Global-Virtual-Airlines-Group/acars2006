// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

/**
 * An interface to describe Envelopes that can be handled by
 * the Network Writer worker. 
 * @author Luke
 * @version 4.0
 * @since 4.0
 */

public interface OutputEnvelope {
	
	/**
	 * Returns the envelope timestamp.
	 * @return the timestamp
	 */
	public long getTime();
	
	/**
	 * Returns the connection ID for this Envelope.
	 * @return the Connection ID
	 */
	public long getConnectionID();
	
	/**
	 * Returns whether this payload is critical.
	 * @return TRUE if the payload is critical, otherwise FALSE
	 */
	public boolean isCritical();
	
	/**
	 * Returns the Envelope payload.
	 * @return the payload
	 */
	public Object getMessage();
}