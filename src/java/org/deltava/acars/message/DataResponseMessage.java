// Copyright 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import java.util.*;

import org.deltava.beans.Pilot;

/**
 * An ACARS data response message bean.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public abstract class DataResponseMessage<E> extends DataMessage {

	// Response data
	private final List<E> _rspData = new ArrayList<E>();
	private long _parent;

	/**
	 * Creates a new Data Response Message.
	 * @param rType the message type
	 * @param msgFrom the Data Response type
	 * @param parentID the parent message ID
	 */
	public DataResponseMessage(Pilot msgFrom, int rType, long parentID) {
		super(Message.MSG_DATARSP, msgFrom);
		setRequestType(rType);
		if (parentID > 0)
			_parent = parentID;
	}

	public long getParentID() {
		return _parent;
	}

	public void add(E obj) {
		// Check if we're not already in the response
		if (obj == null)
			return;

		_rspData.add(obj);
	}
	
	public void addAll(Collection<E> entries) {
		if (entries != null)
			_rspData.addAll(entries);
	}

	public List<E> getResponse() {
		return _rspData;
	}
}