// Copyright 2007, 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import org.jdom.Element;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.dispatch.AcceptMessage;

/**
 * A parser for DispatchAccept elements.
 * @author Luke
 * @version 2.2
 * @since 2.0
 */

class DispatchAcceptParser extends ElementParser<AcceptMessage> {

	/**
	 * Convert an XML dispatch acceptance element into a DispatchAcceptMessage.
	 * @param e the XML element
	 * @param user the originating user
	 * @return an AcceptMessage
	 */
	public AcceptMessage parse(Element e, Pilot user) {
		
		// Create the message
		AcceptMessage msg = new AcceptMessage(user, Long.parseLong(getChildText(e, "parentID", "0"), 16));
		msg.setRecipient(getChildText(e, "recipient", null));
		return msg;
	}
}