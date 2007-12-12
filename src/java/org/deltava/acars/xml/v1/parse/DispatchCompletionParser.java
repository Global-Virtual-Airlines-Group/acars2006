// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import org.jdom.Element;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.dispatch.CompleteMessage;

/**
 * A parser for DispatchCompletion messages. 
 * @author Luke
 * @version 2.0
 * @since 2.0
 */

public class DispatchCompletionParser extends ElementParser {

	/**
	 * Convert an XML dispatch complete element into a DispatchCompleteMessage.
	 * @param e the XML element
	 * @param user the originating user
	 * @return a CancelMessage
	 */
	public Message parse(Element e, Pilot user) {
		return new CompleteMessage(user);
	}
}