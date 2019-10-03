// Copyright 2011, 2012, 2016, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.WarnMessage;
import org.deltava.acars.xml.XMLElementParser;

/**
 * A Parser for Warning elements.
 * @author Luke
 * @version 8.7
 * @since 4.0
 */

class WarnParser extends XMLElementParser<WarnMessage> {

	/**
	 * Convert an XML warn element into a WarnMessage.
	 * @param e the XML element
	 * @return a WarnMessage
	 */
	@Override
	public WarnMessage parse(org.jdom2.Element e, Pilot user) {

		// Create the bean and set the receipients
		WarnMessage msg = new WarnMessage(user, false);
		msg.setRecipient(getChildText(e, "to", null));
		return msg;
	}
}