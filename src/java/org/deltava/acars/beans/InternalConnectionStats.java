// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

/**
 * An updateable class to track ACARS connection statistics. 
 * @author Luke
 * @version 4.0
 * @since 4.0
 */

public class InternalConnectionStats extends ConnectionStats {

	/**
	 * Initializes the class.
	 * @param id the connection ID
	 */
	InternalConnectionStats(long id) {
		super(id);
	}

	void addMessageIn() {
		_msgsIn++;
	}
	
	void addMessageOut() {
		_msgsOut++;
	}
	
	void addPacketIn() {
		_pktsIn++;
	}
	
	void addPacketOut() {
		_pktsOut++;
	}
	
	void addBytesIn(int bytes) {
		_bytesIn += bytes;
	}
	
	void addBytesOut(int bytes) {
		_bytesOut += bytes;
	}
	
	void addVoiceBytesIn(int bytes) {
		_voiceBytesIn += bytes;
	}
	
	void addVoiceBytesOut(int bytes) {
		_voiceBytesOut += bytes;
	}
	
	void addWriteError() {
		_writeErrors++;
	}
	
	void addVoiceWriteError() {
		_voiceWriteErrors++;
	}
}