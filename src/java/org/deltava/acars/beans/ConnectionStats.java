// Copyright 2008, 2010 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

/**
 * An interface to describe object that can return ACARS connection statistics..
 * @author Luke
 * @version 3.1
 * @since 2.1
 */

public interface ConnectionStats extends java.io.Serializable {
	
	/**
	 * Returns the ACARS connection ID.
	 * @return the connection ID
	 */
	public long getID();

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

	/**
	 * Returns the number of write errors on this connection.
	 * @return the number of timed out writes
	 */
	public int getWriteErrors();
}