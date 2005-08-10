package org.deltava.acars.xml;

import java.io.*;
import java.util.*;

import org.jdom.*;
import org.jdom.input.SAXBuilder;

import org.apache.log4j.Logger;

import org.deltava.beans.Pilot;
import org.deltava.acars.beans.Envelope;

import org.deltava.acars.message.Message;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class MessageReader implements Serializable {
	
	private static final Logger log = Logger.getLogger(MessageReader.class);

	private static final String ROOT_ELEMENT_NAME = "ACARSRequest";

	private Document _xdoc;
	private Pilot _sender;
	private long _timeStamp;
	private ArrayList _msgs;
	
	public MessageReader(Envelope env) throws XMLException {
		if (env == null)
			return;
		
		// Build the XML document from the string
		SAXBuilder builder = new SAXBuilder();
		try {
			_xdoc = builder.build(new StringReader((String) env.getMessage()));
			log.debug(env.getMessage());
		} catch (Exception e) {
			throw new XMLException(e.getMessage(), e);
		}
		
		// Get information from the envelope
		_sender = env.getOwner();
		_timeStamp = env.getTime();
	}
	
	private int getMessageType(String mType) {
	
		// Check the message codes
		for (int x = 0, len = Message.MSG_CODES.length; x < len; x++) {
			if (Message.MSG_CODES[x].equals(mType))
				return x;
		}
		
		// Return an invalid code
		return -1;
	}
	
	public Collection parse() throws XMLException {
		
		// If we've already got something, don't parse further and return what we have
		if (_msgs != null)
			return _msgs;
		
		// Create the message store
		_msgs = new ArrayList();
		
		// Get the root element
		Element root = _xdoc.getRootElement();
		if (root == null)
			throw new XMLException("Empty XML Message");
			
		// Check the element name
		if (!ROOT_ELEMENT_NAME.equals(root.getName()))
			throw new XMLException("Invalid root Element - " + root.getName());
		
		// Get the protocol version attribute
		int version = 1;
		try {
			version = Integer.parseInt(root.getAttributeValue("version"));
		} catch (NumberFormatException nfe) {
			throw new XMLException("Invalid ACARS protocol version");
		} catch (NullPointerException npe) {
			throw new XMLException("Missing ACARS protocol version"); 
		} catch (Exception e) {
			throw new XMLException(e.getClass().getName() + " - " + e.getMessage());
		}
		
		// Get the message parsing object
		MessageParser parser = null;
		try {
			String parserClass = getClass().getPackage().getName() + ".MessageParserV" + String.valueOf(version);
			Class pClass = Class.forName(parserClass);
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
		Iterator cmds = root.getChildren("CMD").iterator();
		while (cmds.hasNext()) {
			Element cmdE = (Element) cmds.next();
			
			// Get message type
			int msgType = getMessageType(cmdE.getAttributeValue("type"));
			Message msg = null;
			try {
				parser.setElement(cmdE);
				msg = parser.parse(msgType);
				
				// If we have a message, stamp the ID and add it to the list
				if (msg != null) {
					msg.setID(id);
					_msgs.add(msg);
				}
			} catch (XMLException xe) {
				log.error("Message parse exception - " + xe.getMessage(), xe);
			}
		}
		
		// Return the message list
		return _msgs;
	}
}