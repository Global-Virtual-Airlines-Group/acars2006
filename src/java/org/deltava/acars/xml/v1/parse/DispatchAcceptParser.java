// Copyright 2007, 2008, 2009, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.dispatch.AcceptMessage;
import org.deltava.acars.xml.XMLElementParser;

/**
 * A parser for DispatchAccept elements.
 * @author Luke
 * @version 4.2
 * @since 2.0
 */

class DispatchAcceptParser extends XMLElementParser<AcceptMessage> {

	/**
	 * Convert an XML dispatch acceptance element into a DispatchAcceptMessage.
	 * @param e the XML element
	 * @param user the originating user
	 * @return an AcceptMessage
	 */
	@Override
	public AcceptMessage parse(org.jdom2.Element e, Pilot user) {
		AcceptMessage msg = new AcceptMessage(user, Long.parseLong(getChildText(e, "parentID", "0"), 16));
		msg.setRecipient(getChildText(e, "recipient", null));
		return msg;
	}
}