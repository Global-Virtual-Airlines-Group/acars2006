// Copyright 2011, 2012, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.SwitchChannelMessage;

import org.deltava.acars.xml.*;

/**
 * A message parser for voice channel switch messages.
 * @author Luke
 * @version 7.2
 * @since 4.0
 */

public class SwitchChannelParser extends XMLElementParser<SwitchChannelMessage> {
	
	/**
	 * Convert an XML swchannel element into a SwitchChannelMessage.
	 * @param e the XML element
	 * @return a SwitchChannelMessage
	 */
	@Override
	public SwitchChannelMessage parse(org.jdom2.Element e, Pilot user) {
		
		SwitchChannelMessage msg = new SwitchChannelMessage(user, getChildText(e, "channel", null));
		msg.setDescription(getChildText(e, "desc", null));
		msg.setFrequency(getChildText(e, "freq", null));
		msg.setRecipient(getChildText(e, "recipient", null));
		return msg;
	}
}