// Copyright 2005, 2006, 2007, 2008, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * A message sent when a user disconnects. Unlike most ACARS messages, this is <i>NEVER</i> generated
 * by the client, and is used internally by the server.
 * @author Luke
 * @version 4.0
 * @since 1.0
 * @see org.deltava.acars.message.mp.RemoveMessage
 */

public class QuitMessage extends AbstractMessage {
   
   private int _flightID;
   private boolean _isHidden;
   private boolean _isDispatch;
   private boolean _isMP;
   private boolean _isVoice;

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
	
	public boolean isMP() {
		return _isMP;
	}
	
	public boolean isVoice() {
		return _isVoice;
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
	
	public void setMP(boolean isMP) {
		_isMP = isMP;
	}
	
	public void setVoice(boolean isVoice) {
		_isVoice = isVoice;
	}
}