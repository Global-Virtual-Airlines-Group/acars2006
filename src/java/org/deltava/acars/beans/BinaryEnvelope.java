// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import org.deltava.beans.Pilot;

/**
 * An envelope for binary voice data.
 * @author Luke
 * @version 4.0
 * @since 4.0
 */

public class BinaryEnvelope extends Envelope<byte[]> implements OutputEnvelope {
	
	/**
	 * Initializes the Envelope.
	 * @param usrInfo the author's Pilot object
	 * @param data the voice data
	 * @param conID the Connection ID
	 */
	public BinaryEnvelope(Pilot usrInfo, byte[] data, long conID) {
		super(data, usrInfo, System.nanoTime(), conID);
	}
}