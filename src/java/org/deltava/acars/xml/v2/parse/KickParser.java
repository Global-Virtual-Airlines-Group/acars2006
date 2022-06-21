// Copyright 2019, 2022 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.KickMessage;
import org.deltava.acars.xml.XMLElementParser;

/**
 * A Parser for Disconnect elements.
 * @author Luke
 * @version 10.2
 * @since 8.7
 */

class KickParser extends XMLElementParser<KickMessage> {

	/**
	 * Convert an XML kick element into a KickMessage.
	 * @param e the XML element
	 * @return a KickMessage
	 */
	@Override
	public KickMessage parse(org.jdom2.Element e, Pilot user) {
		
		// Create the bean and set the receipients
		KickMessage msg = new KickMessage(user, Boolean.parseBoolean(getChildText(e, "block", "false")));
		msg.setRecipient(getChildText(e, "to", null));
		return msg;
	}
}