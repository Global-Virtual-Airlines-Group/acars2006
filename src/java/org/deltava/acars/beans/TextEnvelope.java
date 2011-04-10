// Copyright 2006, 2007, 2008, 2009, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import org.deltava.beans.Pilot;

/**
 * An envelope for XML text.
 * @author Luke
 * @version 3.6
 * @since 1.0
 */

public class TextEnvelope extends Envelope<String> {
	
	private int _version;

	/**
	 * Initializes the Envelope
	 * @param usrInfo the author's Pilot object
	 * @param msgText the XML text
	 * @param conID the Connection ID
	 */
	public TextEnvelope(Pilot usrInfo, String msgText, long conID) {
		super(msgText, usrInfo, System.nanoTime(), conID);
	}
	
	/**
	 * Returns the XML protocol version.
	 * @return the protocol version
	 */
	public int getVersion() {
		return _version;
	}

	/**
	 * Updates the XML protocol version.
	 * @param version the protocol version
	 */
	public void setVersion(int version) {
		_version = Math.max(1, version);
	}
	
	/**
	 * Updates the envelope timestamp.
	 * @param time the timestamp
	 */
	public void setTime(long time) {
		_timeStamp = time;
	}
}