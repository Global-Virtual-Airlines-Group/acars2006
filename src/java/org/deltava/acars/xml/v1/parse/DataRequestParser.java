// Copyright 2004, 2005, 2006, 2008, 2009, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import java.util.Iterator;

import org.jdom2.Element;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.*;
import org.deltava.acars.xml.*;

/**
 * A parser for Data Request elements.
 * @author Luke
 * @version 4.2
 * @since 1.0
 */

class DataRequestParser extends XMLElementParser<DataRequestMessage> {

	/**
	 * Convert an XML data request element into a DataRequestMessage.
	 * @param e the XML element
	 * @return a DataRequestMessage
	 * @throws XMLException if a parse error occurs 
	 */
	@Override
	public DataRequestMessage parse(Element e, Pilot user) throws XMLException {

		// Get the request type and validate
		String rType = getChildText(e, "reqtype", null);
		if (rType == null)
			throw new XMLException("Invalid Data Request Type");

		// Create the message
		DataRequestMessage msg = new DataRequestMessage(user, rType);

		// Load the flags
		Element flagsE = e.getChild("flags");
		if (flagsE != null) {
			for (Iterator<Element> i = flagsE.getChildren().iterator(); i.hasNext();) {
				Element fe = i.next();
				msg.addFlag(fe.getName(), fe.getTextTrim());
			}
		}

		return msg;
	}
}