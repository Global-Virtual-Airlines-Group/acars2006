// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import org.deltava.beans.acars.ConnectionStats;

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
	InternalConnectionStats(String id) {
		super(id);
	}

	void addMessageIn() {
		_msgsIn++;
	}
	
	void addMessageOut() {
		_msgsOut++;
	}
	
	void addBufferRead() {
		_bufferReads++;
	}
	
	void addBufferWrite() {
		_bufferWrites++;
	}
	
	void addBytesIn(int bytes) {
		_bytesIn += bytes;
	}
	
	void addBytesOut(int bytes) {
		_bytesOut += bytes;
	}
	
	void addWriteError() {
		_writeErrors++;
	}
}