// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2011, 2015, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * An interface to store common ACARS message data.
 * @author Luke
 * @version 8.3
 * @since 1.0
 */

public interface Message {

	/**
	 * System user name.
	 */
	public static final String SYSTEM_NAME = "SYSTEM";

	/**
	 * Returns the message type.
	 * @return the MessageType
	 */
	public MessageType getType();
	
	/**
	 * Returns if the message can be sent by an unauthenticated user.
	 * @return TRUE if can be sent by an unauthenticated user, otherwise FALSE
	 */
	public boolean isAnonymous();
	
	/**
	 * Sets the time the message was received.
	 * @param ts a timestamp
	 */
	public void setTime(long ts);
	
	/**
	 * Returns the time the message was received.
	 * @return a timestamp
	 */
	public long getTime();
	
	/**
	 * Returns the message protocol version.
	 * @return the protocol version
	 */
	public int getProtocolVersion();

	/**
	 * Updates the message ID.
	 * @param id the ID
	 */
	public void setID(long id);
	
	/**
	 * Returns the message ID.
	 * @return the ID
	 */
	public long getID();
	
	/**
	 * Updates the message sender.
	 * @param msgFrom the sending Pilot
	 */
	public void setSender(Pilot msgFrom);
	
	/**
	 * Returns the message sender.
	 * @return the sending Pilot, or null if unauthenticated
	 */
	public Pilot getSender();
	
	/**
	 * Returns the message sender's Pilot ID.
	 * @return the Pilot ID, or null if unathenticated
	 */
	public String getSenderID();
}