// Copyright 2005, 2006, 2016, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import java.util.*;

import org.deltava.beans.Pilot;

/**
 * An ACARS data response message bean.
 * @author Luke
 * @version 8.4
 * @since 1.0
 * @param <E> the response message type
 */

public abstract class DataResponseMessage<E> extends DataMessage {

	// Response data
	private final List<E> _rspData = new ArrayList<E>();
	private final long _parent;

	/**
	 * Creates a new Data Response Message.
	 * @param rType the message type
	 * @param msgFrom the Data Response type
	 * @param parentID the parent message ID
	 */
	public DataResponseMessage(Pilot msgFrom, DataRequest rType, long parentID) {
		super(MessageType.DATARSP, msgFrom);
		setRequestType(rType);
		_parent = Math.max(0, parentID);
	}

	/**
	 * Returns the ID of the parent message.
	 * @return the ID
	 */
	public long getParentID() {
		return _parent;
	}

	/**
	 * Adds an entry to this response.
	 * @param obj the entry
	 */
	public void add(E obj) {
		if (obj != null)
			_rspData.add(obj);
	}

	/**
	 * Adds multiple entries to the response.
	 * @param entries a Collection of entries
	 */
	public void addAll(Collection<E> entries) {
		if (entries != null)
			_rspData.addAll(entries);
	}

	/**
	 * Returns the response Collection.
	 * @return the response
	 */
	public List<E> getResponse() {
		return _rspData;
	}
}