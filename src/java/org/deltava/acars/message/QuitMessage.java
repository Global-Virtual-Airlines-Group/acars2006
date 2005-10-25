// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class QuitMessage extends AbstractMessage {
   
   private int _flightID;

	/**
	 * Creates a new Quit Message.
	 * @param msgFrom
	 */
	public QuitMessage(Pilot msgFrom) {
		super(Message.MSG_QUIT, msgFrom);
	}
	
	public int getFlightID() {
	   return _flightID;
	}
	
	public void setFlightID(int id) {
	   _flightID = id;
	}
}