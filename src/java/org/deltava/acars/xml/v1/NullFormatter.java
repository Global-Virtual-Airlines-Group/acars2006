// Copyright 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1;

/**
 * A Dummy element formatter to swallow messages. 
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

class NullFormatter extends ElementFormatter {

	/**
	 * Formats a Message bean into an XML element.
	 * @param msg the Message
	 * @return null
	 */
	public org.jdom.Element format(org.deltava.acars.message.Message msg) {
		return null;
	}
}