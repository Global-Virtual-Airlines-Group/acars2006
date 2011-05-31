// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import org.deltava.acars.message.MuteMessage;

import org.deltava.acars.xml.*;
import org.deltava.beans.Pilot;

/**
 * A message parser for voice mute messages.
 * @author Luke
 * @version 4.0
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
	public MuteMessage parse(org.jdom.Element e, Pilot user) {
		
		MuteMessage msg = new MuteMessage(user, getChildText(e, "recipient", null));
		return msg;
	}
}