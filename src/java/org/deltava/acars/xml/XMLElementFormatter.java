// Copyright 2006, 2007, 2009, 2011, 2012, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml;

import org.jdom2.Element;

import org.deltava.acars.message.*;
import org.deltava.acars.xml.ProtocolInfo;

import org.deltava.util.*;

/**
 * A formatter to create XML command elements.
 * @author Luke
 * @version 8.4
 * @since 1.0
 */

public abstract class XMLElementFormatter {

	/**
	 * Formats a Message bean into an XML element.
	 * @param msg the Message
	 * @return an XML element
	 */
	public abstract Element format(Message msg);
	
	/**
	 * Helper method to initialize the response element.
	 */
	protected static Element initResponse(MessageType msgType) {
		Element e = new Element(ProtocolInfo.CMD_ELEMENT_NAME);
		e.setAttribute("type", msgType.getCode());
		return e;
	}
	
	/**
	 * Helper method to initialize a DataResponse element.
	 */
	protected static Element initDataResponse(Element cmd, String rspType) {
		// Get the element
		Element e = cmd.getChild(rspType);
		if (e != null)
			return e;

		// Create the new element
		cmd.addContent(XMLUtils.createElement("rsptype", rspType));
		e = new Element(rspType);
		cmd.addContent(e);
		return e;
	}
}