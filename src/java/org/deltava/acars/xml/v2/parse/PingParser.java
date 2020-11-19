// Copyright 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import java.time.*;
import java.time.format.*;
import java.time.temporal.ChronoField;

import org.deltava.acars.message.PingMessage;
import org.deltava.acars.xml.*;

import org.deltava.beans.Pilot;

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
	public PingMessage parse(org.jdom2.Element e, Pilot user) throws XMLException {
		
		// Create the message
		PingMessage msg = new PingMessage(user);
		
		// Get the local time
		String utc = getChildText(e, "localUTC", null);
		if (utc != null) {
			try {
				DateTimeFormatter mdtf = new DateTimeFormatterBuilder().appendPattern("MM/dd/yyyy HH:mm:ss").appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true).toFormatter();
				Instant utcDate = LocalDateTime.parse(utc.replace('-', '/'), mdtf).toInstant(ZoneOffset.UTC);
				msg.setClientUTC(utcDate);
			} catch (IllegalArgumentException iae) {
				throw new XMLException("Unparseable date/time - " + utc);
			}
		}
		
		return msg;
	}
}