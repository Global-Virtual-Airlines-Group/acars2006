// Copyright 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import org.jdom.Element;

import org.deltava.beans.Pilot;

import org.deltava.acars.xml.XMLElementParser;
import org.deltava.acars.message.viewer.CancelMessage;

/**
 * A parser for Flight Viewer cancellation messages. 
 * @author Luke
 * @version 2.8
 * @since 2.8
 */

class ViewerCancelParser extends XMLElementParser<CancelMessage> {

	/**
	 * Convert an XML viewer request element into a RequestMessage.
	 * @param e the XML element
	 * @return a RequestMessage
	 */
	public CancelMessage parse(Element e, Pilot usr) {
		CancelMessage msg = new CancelMessage(usr);
		msg.setRecipient(getChildText(e, "recipient", ""));
		return msg;
	}
}