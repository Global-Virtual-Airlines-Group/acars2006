// Copyright 2015, 2018, 2022 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.*;

/**
 * An ACARS message to enable/disable data compression.
 * @author Luke
 * @version 10.3
 * @since 6.4
 */

public class CompressionMessage extends AbstractMessage {
	
	private final Compression _type;

	/**
	 * Creates the message.
	 * @param msgFrom the originating Pilot
	 * @param t the Compression type
	 */
	public CompressionMessage(Pilot msgFrom, Compression t) {
		super(MessageType.COMPRESS, msgFrom);
		setProtocolVersion(2);
		_type = t;
	}

	/**
	 * Returns the requested compression tye.
	 * @return the Compression type
	 */
	public Compression getCompression() {
		return _type;
	}
}