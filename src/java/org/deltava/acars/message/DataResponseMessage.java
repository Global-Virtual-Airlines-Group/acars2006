// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.message;

import java.util.*;

import org.deltava.beans.Pilot;

/**
 * An ACARS data response message bean.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class DataResponseMessage extends DataMessage {

	public class DataElement {

		private String _name;

		private Object _value;

		public DataElement(String name, String value) {
			super();
			_name = name;
			_value = value;
		}

		public DataElement(String name, Collection value) {
			super();
			_name = name;
			_value = value;
		}

		public String getName() {
			return _name;
		}

		public Object getValue() {
			return _value;
		}
	}

	// Response data
	private List _rspData = new ArrayList();

	/**
	 * Creates a new Data Response Message
	 * @param rType
	 * @param msgFrom
	 */
	public DataResponseMessage(Pilot msgFrom, int rType) {
		super(Message.MSG_DATARSP, msgFrom);
		setRequestType(rType);
	}

	public DataResponseMessage(Pilot msgFrom, String rType) {
		super(Message.MSG_DATARSP, msgFrom);
		setRequestType(rType);
	}

	public void addResponse(Object obj) {
		// Check if we're not already in the response
		if (obj == null)
			return;

		_rspData.add(obj);
	}

	public void addResponse(String name, String value) {
		addResponse(new DataElement(name, value));
	}

	public void addResponse(String name, Collection value) {
		addResponse(new DataElement(name, value));
	}

	public Collection getResponse() {
		return _rspData;
	}
}