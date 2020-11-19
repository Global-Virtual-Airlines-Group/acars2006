// Copyright 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import org.deltava.acars.message.PingMessage;
import org.deltava.acars.xml.*;

/**
 * A parser for ACARS ping messages.
 * @author Luke
 * @version 9.1
 * @since 9.1
 */

public class PingParser extends XMLElementParser<PingMessage> {
	
	/**
	 * Convert an XML ping element into a PingMessage.
	 * @param e the XML element
	 * @return a PingMessage
	 * @throws XMLException if a parse error occurs 
	 */
	@Override
	public PingMessage parse(org.jdom2.Element e, org.deltava.beans.Pilot user) throws XMLException {
		return new PingMessage(user);
	}
}