// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class QuitMessage extends AbstractMessage {

	/**
	 * @param type
	 * @param msgFrom
	 */
	public QuitMessage(Pilot msgFrom) {
		super(Message.MSG_QUIT, msgFrom);
	}
}