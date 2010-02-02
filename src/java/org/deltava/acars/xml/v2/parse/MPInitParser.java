// Copyright 2010 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import org.deltava.beans.Pilot;
import org.deltava.util.StringUtils;

import org.deltava.acars.message.mp.InitMessage;

import org.deltava.acars.xml.*;

/**
 * A message parser for multi-player initialization messages. 
 * @author Luke
 * @version 3.0
 * @since 3.0
 */

class MPInitParser extends XMLElementParser<InitMessage> {

	/**
	 * Convert an XML MP init element into an MPInitMessage.
	 * @param e the XML element
	 * @return an MPInitMessage
	 * @throws XMLException if a parse error occurs 
	 */
	public InitMessage parse(org.jdom.Element e, Pilot usr) {
		
		// Create the message
		InitMessage msg = new InitMessage(usr);
		msg.setRange(StringUtils.parse(getChildText(e, "range", "40"), 40));
		return msg;
	}
}