// Copyright 2005, 2006, 2007, 2008, 2011, 2018, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.acars.beans.ACARSConnection;

/**
 * A message sent when a user disconnects. Unlike most ACARS messages, this is <i>NEVER</i> generated
 * by the client, and is used internally by the server.
 * @author Luke
 * @version 8.7
 * @since 1.0
 * @see org.deltava.acars.message.mp.RemoveMessage
 */

public class QuitMessage extends AbstractMessage {
   
   private final int _flightID;
   private final boolean _isHidden;
   private final boolean _isDispatch;
   private final boolean _isMP;
   private final boolean _isVoice;

	/**
	 * Creates a new Quit Message.
	 * @param ac the originating ACARSConnection
	 */
	public QuitMessage(ACARSConnection ac) {
		super(MessageType.QUIT, ac.getUser());
		_flightID = ac.getFlightID();
		_isHidden = ac.getUserHidden();
		_isDispatch = ac.getIsDispatch();
		_isMP = ac.getIsMP();
		_isVoice = ac.isVoiceEnabled() || ac.getMuted();
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
}