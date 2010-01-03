// Copyright 2007, 2008, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import org.jdom.Element;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.dispatch.CompleteMessage;
import org.deltava.acars.xml.XMLElementParser;

/**
 * A parser for DispatchCompletion messages. 
 * @author Luke
 * @version 2.8
 * @since 2.0
 */

class DispatchCompletionParser extends XMLElementParser<CompleteMessage> {

	/**
	 * Convert an XML dispatch complete element into a DispatchCompleteMessage.
	 * @param e the XML element
	 * @param user the originating user
	 * @return a CancelMessage
	 */
	public CompleteMessage parse(Element e, Pilot user) {
		CompleteMessage msg = new CompleteMessage(user);
		msg.setRecipient(getChildText(e, "recipient", null));
		return msg;
	}
}