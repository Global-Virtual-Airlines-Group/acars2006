// Copyright 2004, 2005, 2006, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml;

import org.apache.log4j.Logger;

import org.deltava.acars.message.Message;

/**
 * An ACARS Message Formatter. Message Formatters are a way of translating Message objects into
 * XML messages in a protocol version-specific way.
 * @author Luke
 * @version 4.2
 * @since 1.0
 * @see MessageParser
 */

public abstract class MessageFormatter {
	
	protected final Logger log;
	private final int _version;

	/**
	 * Initializes the Message Formatter.
	 * @param version the protocol version supported
	 */
	protected MessageFormatter(int version) {
		super();
		_version = version;
		log = Logger.getLogger(MessageFormatter.class.getName() + "v" + version);
	}
	
	/**
	 * Returns the protocol version supported by this Formatter.
	 * @return the protocol version
	 */
	public final int getProtocolVersion() {
		return _version;
	}
	
	/**
	 * Formats a Message into an XML element.
	 * @param msgBean the Message to format
	 * @return the XML element
	 * @throws XMLException if an error occurs
	 */
	public abstract org.jdom2.Element format(Message msgBean) throws XMLException;
}