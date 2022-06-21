// Copyright 2011, 2012, 2016, 2022 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.VoiceToggleMessage;
import org.deltava.acars.xml.*;

/**
 * A message parser for voice toggle messages.
 * @author Luke
 * @version 10.2
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

		boolean isEnabled = Boolean.parseBoolean(getChildText(e, "enabled", "false"));
		boolean isEcho = Boolean.parseBoolean(getChildText(e, "echo", "false"));
		return new VoiceToggleMessage(user, isEnabled, isEcho);
	}
}