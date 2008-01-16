// Copyright 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

/**
 * A bean to store statistics about a disconnected ACARS connection. 
 * @author Luke
 * @version 2.1
 * @since 2.1
 */

public class DisconnectionStats implements ConnectionStats {

	private long _id;
	private int _msgsIn;
	private int _msgsOut;
	
	private long _bytesIn;
	private long _bytesOut;
	
	/**
	 * Initializes the bean.
	 * @param id the Connection ID
	 */
	public DisconnectionStats(long id) {
		super();
		_id = id;
	}
	
	/**
	 * Returns the connection ID.
	 * @return the ID
	 */
	public long getID() {
		return _id;
	}

	public long getBytesIn() {
		return _bytesIn;
	}

	public long getBytesOut() {
		return _bytesOut;
	}

	public int getMsgsIn() {
		return _msgsIn;
	}

	public int getMsgsOut() {
		return _msgsOut;
	}
	
	/**
	 * Updates the number of messages.
	 * @param msgsIn the number of inbound messages
	 * @param msgsOut the number of outbound messages
	 */
	public void setMessages(int msgsIn, int msgsOut) {
		_msgsIn = Math.max(0, msgsIn);
		_msgsOut = Math.max(0, msgsOut);
	}
	
	/**
	 * Updates the bandwidth totals.
	 * @param bytesIn the number of inbound bytes
	 * @param bytesOut the number of outbound bytes
	 */
	public void setBytes(long bytesIn, long bytesOut) {
		_bytesIn = Math.max(0, bytesIn);
		_bytesOut = Math.max(0, bytesOut);
	}

	public int hashCode() {
		return new Long(_id).hashCode();
	}
}