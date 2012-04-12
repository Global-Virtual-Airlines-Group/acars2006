// Copyright 2004, 2009, 2012 Global Virtual Airlines Group. All Rights Reserved.
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
 * @version 4.2
 * @since 2.8
 */

public abstract class XMLMessageParser extends MessageParser {
	
	protected final Map<Integer, XMLElementParser<? extends Message>> _eParsers = 
		new HashMap<Integer, XMLElementParser<? extends Message>>();
	protected final Map<Integer, XMLElementParser<? extends DispatchMessage>> _dspParsers = 
		new HashMap<Integer, XMLElementParser<? extends DispatchMessage>>();
	protected final Map<Integer, XMLElementParser<? extends ViewerMessage>> _viewParsers =
		new HashMap<Integer, XMLElementParser<? extends ViewerMessage>>();
	
	protected final SAXBuilder builder = new SAXBuilder(XMLReaders.NONVALIDATING);

	/**
	 * Initializes the parser.
	 * @param version the protocol version
	 */
	public XMLMessageParser(int version) {
		super(version);
		builder.setReuseParser(true);
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
			log.warn("Detected multiple XML headers from " + env.getOwnerID());
			int pos = rawMsg.indexOf(XML_HDR);
			while (pos > -1) {
				int endPos = rawMsg.indexOf(XML_HDR, pos + 1);
				msgs.add(rawMsg.substring(pos, (endPos == -1) ? rawMsg.length() : endPos));
				pos = endPos;
			}
		} else
			msgs.add(rawMsg);

		// Split up the XML
		final Collection<Document> xdocs = new ArrayList<Document>();
		try {
			for (Iterator<String> i = msgs.iterator(); i.hasNext();) {
				String req = i.next();
				xdocs.add(builder.build(new java.io.StringReader(req)));
				if (log.isDebugEnabled())
					log.debug(req);
			}
		} catch (Exception e) {
			throw new XMLException(e.getMessage(), e, rawMsg);
		}
		
		// Parse the XML documents
		final Collection<Message> results = new ArrayList<Message>();
		for (Iterator<Document> i = xdocs.iterator(); i.hasNext();) {
			Document xdoc = i.next();
			Element root = xdoc.getRootElement();
			if (root == null) {
				log.warn("Empty XML Message from " + env.getOwnerID());
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
			for (Iterator<Element> cmds = root.getChildren("CMD").iterator(); cmds.hasNext();) {
				Element cmdE = cmds.next();

				// Get message type
				Message msg = null;
				int msgType = StringUtils.arrayIndexOf(Message.MSG_CODES, cmdE.getAttributeValue("type"));
				try {
					// Depending on the message type, either generate a message or lookup the parser in a map
					switch (msgType) {
						case Message.MSG_ENDFLIGHT:
							msg = new EndFlightMessage(env.getOwner()); 
							break;

						case Message.MSG_PING:
							msg = new PINGMessage(env.getOwner());
							break;

						case Message.MSG_QUIT:
							msg = new QuitMessage(env.getOwner());
							break;
							
						case Message.MSG_DISPATCH:
							String reqType = cmdE.getChildTextTrim("reqtype"); 
							int dspType = StringUtils.arrayIndexOf(DispatchMessage.REQ_TYPES, reqType);
							XMLElementParser<? extends DispatchMessage> dp = _dspParsers.get(Integer.valueOf(dspType));
							if (dp != null)
								msg = dp.parse(cmdE, env.getOwner());
							else
								throw new XMLException("Invalid dispatch message type - " + reqType);
							
							break;
							
						case Message.MSG_VIEWER:
							String vreqType = cmdE.getChildTextTrim("reqtype");
							int viewType = StringUtils.arrayIndexOf(ViewerMessage.REQ_TYPES, vreqType);
							XMLElementParser<? extends ViewerMessage> vp = _viewParsers.get(Integer.valueOf(viewType));
							if (vp != null)
								msg = vp.parse(cmdE, env.getOwner());
							else
								throw new XMLException("Invalid viewer message type - " + vreqType);
							
							break;
							
						default:
							XMLElementParser<? extends Message> ep = _eParsers.get(Integer.valueOf(msgType));
							if (ep != null) {
								msg = ep.parse(cmdE, env.getOwner());
								
								// Get the protocol version
								if (msgType == Message.MSG_AUTH) {
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
					if (e instanceof XMLException)
						log.warn("Message parse exception - " + e.getMessage());
					else
						log.error("Message parse exception - " + e.getMessage(), e);
					
					results.add(new ErrorMessage(env.getOwner(), e.getMessage(), id));
				}
			}
		}
		
		return results;
	}
}