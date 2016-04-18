// Copyright 2011, 2012, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import org.jdom2.Element;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.WarnMessage;

import org.deltava.acars.xml.*;

/**
 * A Parser for Warning reset elements.
 * @author Luke
 * @version 7.0
 * @since 4.0
 */

class WarnResetParser extends XMLElementParser<WarnMessage> {

	/**
	 * Convert an XML warn element into a WarnMessage.
	 * @param e the XML element
	 * @return a WarnMessage
	 * @throws XMLException if a parse error occurs 
	 */
	@Override
	public WarnMessage parse(Element e, Pilot user) throws XMLException {

		// Create the bean and set the receipients
		WarnMessage msg = new WarnMessage(user, true);
		msg.setRecipient(getChildText(e, "to", null));
		return msg;
	}
}