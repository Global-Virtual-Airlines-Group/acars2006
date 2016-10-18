// Copyright 2004, 2006, 2009, 2011, 2012, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml;

import org.jdom2.Element;

import org.deltava.beans.Pilot;
import org.deltava.beans.schedule.Airport;
import org.deltava.util.system.SystemData;

import org.deltava.acars.message.Message; 

/**
 * A parser for XML command elements.
 * @author Luke
 * @version 7.2
 * @since 1.0
 * @param <T> the returned message type
 */

public abstract class XMLElementParser<T extends Message> {

	/**
	 * Convert the XML element into a Message.
	 * @param e the XML element
	 * @param user the message author
	 * @return a Message or null if no message should be generated
	 * @throws XMLException if a parse error occurs 
	 */
	public abstract T parse(Element e, Pilot user) throws XMLException;
	
	/**
	 * Helper method to parse XML text with a default value.
	 */
	protected static String getChildText(Element e, String childName, String defaultValue) {
		String tmp = e.getChildTextTrim(childName);
		return (tmp == null) ? defaultValue : tmp;
	}
	
	/**
	 * Helper method to load an airport.
	 * @param code the ICAO/IATA code
	 * @return an Airport bean
	 * @throws XMLException if the code is unknown
	 */
	protected static Airport getAirport(String code) throws XMLException {
		Airport a = SystemData.getAirport(code);
		if (a == null)
			throw new XMLException("Invalid Airport Code - " + code);

		return a;
	}
}