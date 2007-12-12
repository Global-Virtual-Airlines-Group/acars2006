// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.dispatch.CancelMessage;

/**
 * A parser for DispatchCancel elements.
 * @author Luke
 * @version 2.0
 * @since 2.0
 */

public class DispatchCancelParser extends ElementParser {
	
	/**
	 * Convert an XML dispatch cancel element into a DispatchCancelMessage.
	 * @param e the XML element
	 * @param user the originating user
	 * @return a CancelMessage
	 */
	public Message parse(org.jdom.Element e, Pilot user) {
		CancelMessage msg = new CancelMessage(user);
		msg.setRecipient(getChildText(e, "recipient", null));
		return msg;
	}
}