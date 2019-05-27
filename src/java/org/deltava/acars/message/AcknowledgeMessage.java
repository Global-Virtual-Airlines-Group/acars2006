// Copyright 2004, 2005, 2006, 2008, 2018, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import java.util.*;

import org.deltava.beans.Pilot;

/**
 * An ACARS Acknowledgement message.
 * @author Luke
 * @version 8.6
 * @since 1.0
 */

public class AcknowledgeMessage extends AbstractMessage {

	private final long _parent;
	private final Map<String, String> _msgs = new LinkedHashMap<String, String>();

	/**
	 * Creates the message.
	 * @param msgFrom the originating pilot, either the author or the system
	 * @param parentID the parent message ID
	 */
	public AcknowledgeMessage(Pilot msgFrom, long parentID) {
		super(MessageType.ACK, msgFrom);
		_parent = parentID;
	}
	
	/**
	 * Creates an error message.
	 * @param msgFrom the originating pilot, either the author or the system
	 * @param parentID the parent message ID
	 * @param errorMsg the error message
	 */
	public AcknowledgeMessage(Pilot msgFrom, long parentID, String errorMsg) {
		this(msgFrom, parentID);
		setEntry("error", errorMsg);
	}
	
	/**
	 * Returns the parent message ID.
	 * @return the message ID
	 */
	public long getParentID() {
		return _parent;
	}
	
	/**
	 * Retrieves a message attribute.
	 * @param eName the attribute name
	 * @return the attribute value, or null if not found
	 */
	public String getEntry(String eName) {
		return _msgs.get(eName);
	}
	
	/**
	 * Returns the names of all message attributes.
	 * @return a Collection of attribute names
	 */
	public Collection<Map.Entry<String, String>> getEntries() {
		return _msgs.entrySet();
	}
	
	/**
	 * Adds a message attribute. If the attribute already exists, it will be overwritten.
	 * @param eName the attribute name
	 * @param eValue the attribute value
	 */
	public void setEntry(String eName, String eValue) {
		_msgs.put(eName, eValue);
	}
}