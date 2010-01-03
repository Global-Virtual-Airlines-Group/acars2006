// Copyright 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import org.jdom.Element;

import org.deltava.beans.Pilot;

import org.deltava.acars.xml.XMLElementParser;
import org.deltava.acars.message.viewer.AcceptMessage;

/**
 * A parser for Flight Viewer acceptance messages. 
 * @author Luke
 * @version 2.8
 * @since 2.8
 */

class ViewerAcceptParser extends XMLElementParser<AcceptMessage> {

	/**
	 * Convert an XML viewer request element into a RequestMessage.
	 * @param e the XML element
	 * @return a RequestMessage
	 */
	public AcceptMessage parse(Element e, Pilot usr) {
		AcceptMessage msg = new AcceptMessage(usr);
		msg.setRecipient(getChildText(e, "recipient", ""));
		return msg;
	}
}