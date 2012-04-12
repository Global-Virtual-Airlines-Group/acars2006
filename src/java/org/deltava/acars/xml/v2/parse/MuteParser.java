// Copyright 2011, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import org.deltava.acars.message.MuteMessage;

import org.deltava.acars.xml.*;
import org.deltava.beans.Pilot;

/**
 * A message parser for voice mute messages.
 * @author Luke
 * @version 4.2
 * @since 4.0
 */

class MuteParser extends XMLElementParser<MuteMessage> {

	/**
	 * Convert an XML mute element into a MuteMessage.
	 * @param e the XML element
	 * @return a MuteMessage
	 * @throws XMLException if a parse error occurs 
	 */
	@Override
	public MuteMessage parse(org.jdom2.Element e, Pilot user) {
		
		return new MuteMessage(user, getChildText(e, "user", null));
	}
}