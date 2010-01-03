// Copyright 2007, 2008, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.dispatch.CancelMessage;
import org.deltava.acars.xml.XMLElementParser;

/**
 * A parser for DispatchCancel elements.
 * @author Luke
 * @version 2.8
 * @since 2.0
 */

class DispatchCancelParser extends XMLElementParser<CancelMessage> {
	
	/**
	 * Convert an XML dispatch cancel element into a DispatchCancelMessage.
	 * @param e the XML element
	 * @param user the originating user
	 * @return a CancelMessage
	 */
	public CancelMessage parse(org.jdom.Element e, Pilot user) {
		CancelMessage msg = new CancelMessage(user);
		msg.setRecipient(getChildText(e, "recipient", null));
		return msg;
	}
}