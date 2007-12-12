// Copyright 2005, 2006, 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * A message sent when a user disconnects. Unlike most ACARS messages, this is <i>NEVER</i> generated
 * by the client, and is used internally by the server.
 * @author Luke
 * @version 2.0
 * @since 1.0
 */

public class QuitMessage extends AbstractMessage {
   
   private int _flightID;
   private boolean _isHidden;
   private boolean _isDispatch;

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
	
	public boolean isDispatch() {
		return _isDispatch;
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
	
	public void setDispatch(boolean isDispatch) {
		_isDispatch = isDispatch;
	}
}