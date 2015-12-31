// Copyright 2015 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import org.deltava.beans.Pilot;
import org.deltava.acars.beans.Compression;
import org.deltava.acars.message.CompressionMessage;
import org.deltava.acars.xml.*;

/**
 * A parser for ACARS
 * @author Luke
 * @version 6.4
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
		try {
			Compression c = Compression.valueOf(getChildText(e, "type", "none").toUpperCase());
			return new CompressionMessage(user, c);
		} catch (IllegalArgumentException iae) {
			throw new XMLException(iae.getMessage(), iae);
		}
	}
}