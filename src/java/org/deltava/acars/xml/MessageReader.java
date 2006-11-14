// Copyright 2004, 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml;

import java.io.*;
import java.util.*;

import org.jdom.*;
import org.jdom.input.SAXBuilder;

import org.apache.log4j.Logger;

import org.deltava.beans.Pilot;
import org.deltava.acars.beans.Envelope;

import org.deltava.acars.message.Message;
import org.deltava.acars.message.ErrorMessage;

import org.deltava.util.StringUtils;

/**
 * A class to translate XML messages into objects.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class MessageReader {

	private static final Logger log = Logger.getLogger(MessageReader.class);
	private static final Map<Integer, Class> _readers = new HashMap<Integer, Class>();

	private static final String ROOT_ELEMENT_NAME = "ACARSRequest";
	private static final String XML_HDR = "<?xml";

	private final Collection<Document> _xdocs = new ArrayList<Document>();
	private Pilot _sender;
	private long _timeStamp;

	private final Collection<Message> _msgs = new ArrayList<Message>();
	private boolean _isLoaded;

	/**
	 * Initialzies the Message Reader.
	 * @param env the Envelope to read
	 * @throws XMLException if an XML error occurs
	 */
	public MessageReader(Envelope env) throws XMLException {
		if (env == null)
			return;

		// Build the XML document from the string
		String msg = String.valueOf(env.getMessage());
		if (msg == null)
			return;

		// Check for muiltiple XML headers
		final Collection<String> msgs = new ArrayList<String>();
		if (msg.indexOf(XML_HDR) != msg.lastIndexOf(XML_HDR)) {
			log.warn("Detected multiple XML headers from " + env.getOwnerID());
			int pos = msg.indexOf(XML_HDR);
			while (pos > -1) {
				int endPos = msg.indexOf(XML_HDR, pos + 1);
				msgs.add(msg.substring(pos, (endPos == -1) ? msg.length() : endPos));
				pos = endPos;
			}
		} else
			msgs.add(msg);

		// Parse the XML
		try {
			SAXBuilder builder = new SAXBuilder();
			for (Iterator<String> i = msgs.iterator(); i.hasNext();) {
				String req = i.next();
				_xdocs.add(builder.build(new StringReader(req)));
				if (log.isDebugEnabled())
					log.debug(req);
			}
		} catch (Exception e) {
			XMLException xe = new XMLException(e.getMessage(), e);
			xe.setXML(msg);
			throw xe;
		}

		// Get information from the envelope
		_sender = env.getOwner();
		_timeStamp = env.getTime();
	}

	/**
	 * Helper method to load a parser class.
	 */
	private static synchronized Class getParser(int version) {
		Integer v = new Integer(version);
		if (_readers.containsKey(v))
			return _readers.get(v);

		// Instantiate a new version
		String parserClass = MessageReader.class.getPackage().getName() + ".MessageParserV" + String.valueOf(version);
		try {
			Class pClass = Class.forName(parserClass);
			_readers.put(v, pClass);
			return pClass;
		} catch (ClassNotFoundException cnfe) {
			log.error("Cannot create MessageReader for version " + version);
			return null;
		}
	}

	/**
	 * Parses the loaded envelope(s).
	 * @return a Collection of Messages
	 * @throws XMLException if a parsing error occurs
	 */
	public Collection<Message> parse() throws XMLException {

		// If we've already got something, don't parse further and return what we have
		if (_isLoaded)
			return _msgs;

		// Parse the envelopers
		for (Iterator<Document> i = _xdocs.iterator(); i.hasNext();) {
			Document xdoc = i.next();
			Element root = xdoc.getRootElement();
			if (root == null)
				throw new XMLException("Empty XML Message");

			// Check the element name
			if (!ROOT_ELEMENT_NAME.equals(root.getName()))
				throw new XMLException("Invalid root Element - " + root.getName());

			// Get the message parsing object
			MessageParser parser = null;
			try {
				int version = StringUtils.parse(root.getAttributeValue("version"), 1);
				Class pClass = getParser(version);
				parser = (MessageParser) pClass.newInstance();

				// Pass info to the parser
				parser.setUser(_sender);
				parser.setTime(_timeStamp);
			} catch (Exception e) {
				throw new XMLException("Error creating parser - " + e.getClass().getName(), e);
			}

			// Get the message ID
			long id = 0;
			try {
				id = Long.parseLong(root.getAttributeValue("id"), 16);
			} catch (Exception e) {
				throw new XMLException("Invalid Message ID - " + e.getMessage());
			}

			// Get the commands
			for (Iterator cmds = root.getChildren("CMD").iterator(); cmds.hasNext();) {
				Element cmdE = (Element) cmds.next();

				// Get message type
				int msgType = StringUtils.arrayIndexOf(Message.MSG_CODES, cmdE.getAttributeValue("type"));
				try {
					parser.setElement(cmdE);
					Message msg = parser.parse(msgType);

					// If we have a message, stamp the ID and add it to the list
					if (msg != null) {
						msg.setID(id);
						_msgs.add(msg);
					}
				} catch (Exception e) {
					log.warn("Message parse exception - " + e.getMessage());
					_msgs.add(new ErrorMessage(_sender, e.getMessage(), id));
				}
			}
		}

		// Return the message list
		return _msgs;
	}
}