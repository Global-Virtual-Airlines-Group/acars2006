// Copyright 2008, 2010, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

/**
 * A bean to store statistics about an ACARS connection. 
 * @author Luke
 * @version 4.0
 * @since 2.1
 */

public class ACARSConnectionStats extends ConnectionStats {
	
	/**
	 * Initializes the bean.
	 * @param id the connection ID
	 */
	public ACARSConnectionStats(long id) {
		super(id);
	}

	/**
	 * Initializes the bean and copies data from an existing ACARS connection statistics bean.
	 * @param con the ConnectionStats object
	 * @throws NullPointerException if con is null
	 */
	public ACARSConnectionStats(ConnectionStats con) {
		super(con);
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

	/**
	 * Updates the voice bandwidth totals.
	 * @param bytesIn the number of inbound voice bytes
	 * @param bytesOut the number of outbound voice bytes
	 */
	public void setVoiceBytes(long bytesIn, long bytesOut) {
		_voiceBytesIn = Math.max(0, bytesIn);
		_voiceBytesOut = Math.max(0, bytesOut);
	}

	/**
	 * Updates the number of voice packets.
	 * @param msgsIn the number of inbound packets
	 * @param msgsOut the number of outbound packets
	 */
	public void setPackets(int packetsIn, int packetsOut) {
		_pktsIn = Math.max(0, packetsIn);
		_pktsOut = Math.max(0, packetsOut);
	}

	/**
	 * Updates the number of write errors.
	 * @param errors the number of errors
	 */
	public void setWriteErrors(int errors) {
		_writeErrors = Math.max(0, errors);
	}

	/**
	 * Updates the number of voice write errors.
	 * @param errors the number of errors
	 */
	public void setVoiceWriteErrors(int errors) {
		_voiceWriteErrors = Math.max(0, errors);		
	}
}