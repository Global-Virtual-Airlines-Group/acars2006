// Copyright 2006, 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import org.deltava.beans.Pilot;

/**
 * An envelope for XML text.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class TextEnvelope extends Envelope {
	
	private int _version;

	/**
	 * Initializes the Envelope
	 * @param usrInfo the author's Pilot object
	 * @param msgText the XML text
	 * @param conID the Connection ID
	 */
	public TextEnvelope(Pilot usrInfo, String msgText, long conID) {
		super(usrInfo, msgText, conID);
	}
	
	/**
	 * Returns the XML text.
	 * @return the XML text
	 */
	public String getMessage() {
		return (String) _payload;
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
		_version = (version < 1) ? 1 : version;
	}
	
	/**
	 * Updates the envelope timestamp.
	 * @param time the timestamp
	 */
	public void setTime(long time) {
		if (time > 1)
			_timeStamp = time;
	}
}