// Copyright 2004, 2009, 2012, 2016, 2017, 2018, 2023, 2025 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml;

import java.util.*;

import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;

import org.deltava.acars.beans.TextEnvelope;
import org.deltava.acars.message.*;

import org.deltava.util.StringUtils;

/**
 * An abstract class to parse ACARS XML messages.
 * @author Luke
 * @version 12.3
 * @since 2.8
 */

public abstract class XMLMessageParser extends MessageParser {
	
	protected final Map<MessageType, XMLElementParser<? extends Message>> _eParsers =  new HashMap<MessageType, XMLElementParser<? extends Message>>();
	protected final Map<DispatchRequest, XMLElementParser<? extends DispatchMessage>> _dspParsers = new HashMap<DispatchRequest, XMLElementParser<? extends DispatchMessage>>();
	
	protected final SAXBuilder builder = new SAXBuilder(XMLReaders.NONVALIDATING);

	/**
	 * Initializes the parser.
	 * @param version the protocol version
	 */
	public XMLMessageParser(int version) {
		super(version);
		builder.setReuseParser(true);
		builder.setProperty(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, "");
		init();
	}
	
	/**
	 * Initializes the parsers.
	 */
	protected abstract void init();
	
	/**
	 * Parses a message Envelope.
	 * @param env the Envelope
	 * @return a Collection of Message beans
	 * @throws XMLException if an error occurs
	 */
	@Override
	public Collection<Message> parse(TextEnvelope env) throws XMLException {
		if (env == null)
			return Collections.emptySet();

		// Build the XML document from the string
		String rawMsg = env.getMessage();
		if (rawMsg == null)
			return Collections.emptySet();
		
		// Check for muiltiple XML headers
		final Collection<String> msgs = new ArrayList<String>();
		if (rawMsg.indexOf(XML_HDR) != rawMsg.lastIndexOf(XML_HDR)) {
			int pos = rawMsg.indexOf(XML_HDR);
			while (pos > -1) {
				int endPos = rawMsg.indexOf(XML_HDR, pos + 4);
				msgs.add(rawMsg.substring(pos, (endPos == -1) ? rawMsg.length() : endPos));
				pos = endPos;
			}
		} else
			msgs.add(rawMsg);

		// Split up the XML
		final Collection<Document> xdocs = new ArrayList<Document>();
		try {
			for (String req : msgs) {
				xdocs.add(builder.build(new java.io.StringReader(req)));
				if (log.isDebugEnabled())
					log.debug(req);
			}
		} catch (Exception e) {
			throw new XMLException(e.getMessage(), e, rawMsg);
		}
		
		// Parse the XML documents
		final Collection<Message> results = new ArrayList<Message>();
		for (Document xdoc:  xdocs) {
			Element root = xdoc.getRootElement();
			if (root == null) {
				log.warn("Empty XML Message from {}", env.getOwnerID());
				continue;
			}
				
			// Check the element name
			if (!ProtocolInfo.REQ_ELEMENT_NAME.equals(root.getName()))
				throw new XMLException("Invalid root Element - " + root.getName());

			// Get the message ID
			long id = 0;
			try {
				id = Long.parseLong(root.getAttributeValue("id"), 16);
			} catch (Exception e) {
				throw new XMLException("Invalid Message ID - " + e.getMessage());
			}
			
			// Get the commands
			for (Element cmdE: root.getChildren(ProtocolInfo.CMD_ELEMENT_NAME)) {
				Message msg = null;
				MessageType msgType = MessageType.fromType(cmdE.getAttributeValue("type"));
				try {
					// Depending on the message type, either generate a message or lookup the parser in a map
					switch (msgType) {
						case ENDFLIGHT:
							msg = new EndFlightMessage(env.getOwner()); 
							break;

						case DISPATCH:
							String reqType = cmdE.getChildTextTrim("reqtype"); 
							DispatchRequest dspType = DispatchRequest.fromType(reqType);
							XMLElementParser<? extends DispatchMessage> dp = _dspParsers.get(dspType);
							if (dp != null)
								msg = dp.parse(cmdE, env.getOwner());
							else
								throw new XMLException("Invalid dispatch message type - " + reqType);
							
							break;
							
						default:
							XMLElementParser<? extends Message> ep = _eParsers.get(msgType);
							if (ep != null) {
								msg = ep.parse(cmdE, env.getOwner());
								
								// Get the protocol version
								if (msgType == MessageType.AUTH) {
									int version = StringUtils.parse(root.getAttributeValue("version"), getProtocolVersion());
									AuthenticateMessage amsg = (AuthenticateMessage) msg;
									amsg.setRequestedProtocolVersion(version);
								}
							} else
								throw new XMLException("Invalid message type - " + msgType);
					}

					// If we have a message, stamp the ID and add it to the list
					if (msg != null) {
						msg.setTime(env.getTime());
						msg.setID(id);
						results.add(msg);
					}
				} catch (Exception e) {
					log.atError().withThrowable(e).log("Message parse exception - {}", e.getMessage());
					results.add(new ErrorMessage(env.getOwner(), e.getMessage(), id));
				}
			}
		}
		
		return results;
	}
}