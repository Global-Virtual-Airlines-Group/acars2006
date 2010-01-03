// Copyright 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import org.jdom.Element;

import org.deltava.beans.Pilot;

import org.deltava.acars.xml.XMLElementParser;
import org.deltava.acars.message.viewer.RequestMessage;

/**
 * A parser for Flight Viewer request messages. 
 * @author Luke
 * @version 2.8
 * @since 2.8
 */

class ViewerRequestParser extends XMLElementParser<RequestMessage> {

	/**
	 * Convert an XML viewer request element into a RequestMessage.
	 * @param e the XML element
	 * @return a RequestMessage
	 */
	public RequestMessage parse(Element e, Pilot usr) {
		RequestMessage msg = new RequestMessage(usr);
		msg.setRecipient(getChildText(e, "recipient", ""));
		return msg;
	}
}