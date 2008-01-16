// Copyright 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

/**
 * An interface to describe object that can return connection statistics..
 * @author Luke
 * @version 2.1
 * @since 2.1
 */

public interface ConnectionStats extends java.io.Serializable {

	/**
	 * Returns the number of messages received on this connection.
	 * @return the number of messages
	 */
	public int getMsgsIn();
	
	/**
	 * Returns the number of messages sent on this connection.
	 * @return the number of messages
	 */
	public int getMsgsOut();
	
	/**
	 * Returns the number of bytes received on this connection.
	 * @return the number of bytes
	 */
	public long getBytesIn();
	
	/**
	 * Returns the number of bytes sent on this connection.
	 * @return the number of bytes
	 */
	public long getBytesOut();
}