// Copyright 2006, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.*;

/**
 * An ACARS data response message to store generic name/multiple value pairs. This allows the sending of multiple
 * values associated with a single label type; it is not a complete name/value map.
 * @author Luke
 * @version 8.4
 * @since 1.0
 */

public class GenericMessage extends DataResponseMessage<String> {
	
	private String _label;

	/**
	 * Instantiates the Message.
	 * @param msgFrom the originating Pilot
	 * @param rType the request message type
	 * @param parentID the request message ID
	 */
	public GenericMessage(Pilot msgFrom, DataRequest rType, long parentID) {
		super(msgFrom, rType, parentID);
	}
	
	/**
	 * Returns the label.
	 * @return the label
	 */
	public String getLabel() {
		return _label;
	}
	
	/**
	 * Sets the label.
	 * @param label the label
	 */
	public void setLabel(String label) {
		_label = label;
	}
}