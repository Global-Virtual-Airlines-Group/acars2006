// Copyright 2004, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import org.jdom.Element;

import org.apache.log4j.Logger;

import org.deltava.beans.Pilot;

import org.deltava.acars.xml.XMLException;
import org.deltava.acars.message.Message; 

/**
 * A parser for XML command elements.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

abstract class ElementParser {

	protected static final Logger log = Logger.getLogger(ElementParser.class);
	
	/**
	 * Convert the XML element into a Message.
	 * @param e the XML element
	 * @param user the message author
	 * @return a Message or null if no message should be generated
	 * @throws XMLException if a parse error occurs 
	 */
	public abstract Message parse(Element e, Pilot user) throws XMLException;
	
	/**
	 * Helper method to parse XML text with a default value.
	 */
	protected String getChildText(Element e, String childName, String defaultValue) {
		String tmp = e.getChildTextTrim(childName);
		return (tmp == null) ? defaultValue : tmp;
	}
}