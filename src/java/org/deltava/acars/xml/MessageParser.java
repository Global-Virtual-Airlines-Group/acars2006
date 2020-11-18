// Copyright 2004, 2005, 2006, 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml;

import java.util.Collection;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.TextEnvelope;
import org.deltava.acars.message.Message;

/**
 * An interface for ACARS XML message parsers.
 * @author Luke
 * @version 9.1
 * @since 1.0
 * @see MessageFormatter
 */

public abstract class MessageParser {
	
	/**
	 * XML header.
	 */
	protected static final String XML_HDR = "<?xml";
	
	protected final Logger log;
	private final int _version;
	
	/**
	 * Initializes the Message Parser.
	 * @param version the protocol version supported
	 */
	protected MessageParser(int version) {
		super();
		_version = version;
		log = Logger.getLogger(MessageParser.class.getName() + "v" + version);
	}

	/**
	 * Returns the protocol version supported by this Formatter.
	 * @return the protocol version
	 */
	public final int getProtocolVersion() {
		return _version;
	}
	
	/**
	 * Parses a message Envelope.
	 * @param e the Envelope
	 * @return a Collection of Message beans
	 * @throws XMLException if an error occurs
	 */
	public abstract Collection<Message> parse(TextEnvelope e) throws XMLException;
}