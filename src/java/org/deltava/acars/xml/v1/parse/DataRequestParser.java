// Copyright 2004, 2005, 2006, 2008, 2009, 2012, 2016, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import org.jdom2.Element;

import org.deltava.acars.message.*;
import org.deltava.acars.xml.*;

/**
 * A parser for Data Request elements.
 * @author Luke
 * @version 8.4
 * @since 1.0
 */

class DataRequestParser extends XMLElementParser<DataRequestMessage> {

	/**
	 * Convert an XML data request element into a DataRequestMessage.
	 * @param e the XML element
	 * @param user the originating Pilot
	 * @return a DataRequestMessage
	 * @throws XMLException if a parse error occurs 
	 */
	@Override
	public DataRequestMessage parse(Element e, org.deltava.beans.Pilot user) throws XMLException {

		// Get the request type and validate
		String rType = getChildText(e, "reqtype", null);
		DataRequest req = DataRequest.fromType(rType);
		if (req == DataRequest.UNKNOWN)
			throw new XMLException("Invalid Data Request Type");

		// Create the message
		Element flagsE = e.getChild("flags");
		DataRequestMessage msg = new DataRequestMessage(user, req);
		if (flagsE != null)
			flagsE.getChildren().forEach(fe -> msg.addFlag(fe.getName(), fe.getTextTrim()));

		return msg;
	}
}