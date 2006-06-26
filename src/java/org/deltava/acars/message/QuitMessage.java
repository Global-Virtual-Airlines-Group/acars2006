// Copyright 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * A message sent when a user disconnects. Unlike most ACARS messages, this is <i>NEVER</i> generated
 * by the client, and is used internally by the server.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class QuitMessage extends AbstractMessage {
   
   private int _flightID;
   private boolean _isHidden;

	/**
	 * Creates a new Quit Message.
	 * @param msgFrom
	 */
	public QuitMessage(Pilot msgFrom) {
		super(Message.MSG_QUIT, msgFrom);
	}
	
	public boolean isHidden() {
		return _isHidden;
	}
	
	public int getFlightID() {
	   return _flightID;
	}
	
	public void setFlightID(int id) {
	   _flightID = id;
	}
	
	public void setHidden(boolean isHidden) {
		_isHidden = isHidden;
	}
}