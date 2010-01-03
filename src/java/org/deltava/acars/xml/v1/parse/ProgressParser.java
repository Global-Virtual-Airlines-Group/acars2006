// Copyright 2007, 2008, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import org.jdom.Element;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.dispatch.ProgressRequestMessage;
import org.deltava.acars.xml.XMLElementParser;

/**
 * A parser for Dispatch progress request elements.
 * @author Luke
 * @version 2.8
 * @since 2.1
 */

class ProgressParser extends XMLElementParser<ProgressRequestMessage> {

	/**
	 * Convert an XML dispatch progress element into a ProgressRequestMessage.
	 * @param e the XML element
	 * @param user the originating user
	 * @return a ProgressRequestMessage
	 */
	public ProgressRequestMessage parse(Element e, Pilot user) {
		return new ProgressRequestMessage(user, e.getChildTextTrim("pilot"));
	}
}