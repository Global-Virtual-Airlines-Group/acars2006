// Copyright 2011, 2012, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.VoiceToggleMessage;
import org.deltava.acars.xml.*;

/**
 * A message parser for voice toggle messages.
 * @author Luke
 * @version 7.2
 * @since 4.0
 */

public class VoiceToggleParser extends XMLElementParser<VoiceToggleMessage> {

	/**
	 * Convert an XML mute element into a VoiceToggleMessage.
	 * @param e the XML element
	 * @return a VoiceToggleMessage
	 */
	@Override
	public VoiceToggleMessage parse(org.jdom2.Element e, Pilot user) {

		boolean isEnabled = Boolean.valueOf(getChildText(e, "enabled", "false")).booleanValue();
		boolean isEcho = Boolean.valueOf(getChildText(e, "echo", "false")).booleanValue();
		return new VoiceToggleMessage(user, isEnabled, isEcho);
	}
}