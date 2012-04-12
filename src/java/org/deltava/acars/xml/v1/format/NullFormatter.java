// Copyright 2006, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

/**
 * A Dummy element formatter to swallow messages. 
 * @author Luke
 * @version 4.2
 * @since 1.0
 */

class NullFormatter extends ElementFormatter {

	/**
	 * Formats a Message bean into an XML element.
	 * @param msg the Message
	 * @return null
	 */
	@Override
	public org.jdom2.Element format(org.deltava.acars.message.Message msg) {
		return null;
	}
}