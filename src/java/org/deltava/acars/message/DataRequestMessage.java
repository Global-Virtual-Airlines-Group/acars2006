// Copyright 2004, 2005, 2008, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import java.util.*;

import org.deltava.beans.Pilot;

/**
 * An ACARS message to request data from the server.
 * @author Luke
 * @version 8.4
 * @since 1.0
 */

public class DataRequestMessage extends DataMessage {
	
	private final Map<String, String> _flags = new HashMap<String, String>();
	private String _reqFilter = "*";
	
	/**
	 * Initializes the message.
	 * @param msgFrom the originating Pilot
	 * @param rType the data request type
	 */
	public DataRequestMessage(Pilot msgFrom, DataRequest rType) {
		super(MessageType.DATAREQ, msgFrom);
		setRequestType(rType);
	}
	
	/**
	 * Adds a request flag to the message.
	 * @param name the flag name
	 * @param value the flag value
	 */
	public void addFlag(String name, String value) {
		_flags.put(name.toUpperCase(), value);
	}

	/**
	 * Returns the request filter.
	 * @return the request filter
	 */
	public String getFilter() {
		return _reqFilter;
	}
	
	/**
	 * Returns a request flag.
	 * @param name the flag name
	 * @return the flag value, or null if not found
	 */
	public String getFlag(String name) {
		return _flags.get(name.toUpperCase());
	}

	/**
	 * Returns if a request flag is present.
	 * @param flagName the flag name
	 * @return TRUE if the flag is present, otherwise FALSE
	 */
	public boolean hasFlag(String flagName) {
		return _flags.containsKey(flagName.toUpperCase());
	}
	
	/**
	 * Updates the request filter.
	 * @param newFilter the new filter
	 */
	public void setFilter(String newFilter) {
		_reqFilter = (newFilter == null) ? "*" : newFilter;
	}
}