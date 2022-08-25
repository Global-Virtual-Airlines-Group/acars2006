// Copyright 2015, 2022 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import org.deltava.beans.*;
import org.deltava.util.EnumUtils;

import org.deltava.acars.message.CompressionMessage;
import org.deltava.acars.xml.*;

/**
 * A parser for ACARS compression type messages.
 * @author Luke
 * @version 10.3
 * @since 6.4
 */

public class CompressionParser extends XMLElementParser<CompressionMessage> {
	
	/**
	 * Convert an XML ack element into an AcknowledgeMessage.
	 * @param e the XML element
	 * @return an AcknowledgeMessage
	 * @throws XMLException if a parse error occurs 
	 */
	@Override
	public CompressionMessage parse(org.jdom2.Element e, Pilot user) throws XMLException {
		Compression c = EnumUtils.parse(Compression.class, getChildText(e, "type", "none"), Compression.NONE);
		return new CompressionMessage(user, c);
	}
}