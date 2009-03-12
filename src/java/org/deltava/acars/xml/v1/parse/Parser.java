// Copyright 2004, 2005, 2006, 2007, 2008, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import java.util.*;

import org.jdom.*;
import org.jdom.input.SAXBuilder;

import org.deltava.acars.beans.TextEnvelope;
import org.deltava.acars.message.*;
import org.deltava.acars.xml.*;

import org.deltava.util.StringUtils;

/**
 * A parser for ACARS Protocol v1 messages.
 * @author Luke
 * @version 2.4
 * @since 1.0
 */

public class Parser extends org.deltava.acars.xml.MessageParser {

	private final Map<Integer, ElementParser> _eParsers = new HashMap<Integer, ElementParser>();
	private final Map<Integer, ElementParser> _dspParsers = new HashMap<Integer, ElementParser>();
	
	/**
	 * Initializes the Parser.
	 */
	public Parser() {
		super(1);
		_eParsers.put(Integer.valueOf(Message.MSG_MPUPDATE), new MPLocationParser());
		_eParsers.put(Integer.valueOf(Message.MSG_ACK), new AckParser());
		_eParsers.put(Integer.valueOf(Message.MSG_AUTH), new AuthParser());
		_eParsers.put(Integer.valueOf(Message.MSG_DATAREQ), new DataRequestParser());
		_eParsers.put(Integer.valueOf(Message.MSG_DIAG), new DiagnosticParser());
		_eParsers.put(Integer.valueOf(Message.MSG_INFO), new FlightInfoParser());
		_eParsers.put(Integer.valueOf(Message.MSG_PIREP), new FlightReportParser());
		_eParsers.put(Integer.valueOf(Message.MSG_POSITION), new PositionParser());
		_eParsers.put(Integer.valueOf(Message.MSG_TEXT), new TextMessageParser());
		_dspParsers.put(Integer.valueOf(DispatchMessage.DSP_SVCREQ), new DispatchRequestParser());
		_dspParsers.put(Integer.valueOf(DispatchMessage.DSP_CANCEL), new DispatchCancelParser());
		_dspParsers.put(Integer.valueOf(DispatchMessage.DSP_ACCEPT), new DispatchAcceptParser());
		_dspParsers.put(Integer.valueOf(DispatchMessage.DSP_INFO), new DispatchInfoParser());
		_dspParsers.put(Integer.valueOf(DispatchMessage.DSP_ROUTEREQ), new DispatchRouteParser());
		_dspParsers.put(Integer.valueOf(DispatchMessage.DSP_COMPLETE), new DispatchCompletionParser());
		_dspParsers.put(Integer.valueOf(DispatchMessage.DSP_PROGRESS), new ProgressParser());
		_dspParsers.put(Integer.valueOf(DispatchMessage.DSP_RANGE), new DispatchRangeParser());
	}
	
	/**
	 * Parses a message Envelope.
	 * @param env the Envelope
	 * @return a Collection of Message beans
	 * @throws XMLException if an error occurs
	 */
	public final Collection<Message> parse(TextEnvelope env) throws XMLException {
		if (env == null)
			return Collections.emptySet();

		// Build the XML document from the string
		String rawMsg = String.valueOf(env.getMessage());
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
			SAXBuilder builder = new SAXBuilder();
			for (Iterator<String> i = msgs.iterator(); i.hasNext();) {
				String req = i.next();
				xdocs.add(builder.build(new java.io.StringReader(req)));
				if (log.isDebugEnabled())
					log.debug(req);
			}
		} catch (Exception e) {
			XMLException xe = new XMLException(e.getMessage(), e);
			xe.setXML(rawMsg);
			throw xe;
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
			for (Iterator cmds = root.getChildren("CMD").iterator(); cmds.hasNext();) {
				Element cmdE = (Element) cmds.next();

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
							ElementParser dp = _dspParsers.get(Integer.valueOf(dspType));
							if (dp != null)
								msg = dp.parse(cmdE, env.getOwner());
							else
								throw new XMLException("Invalid dispatch message type - " + reqType);
							
							break;

						default:
							ElementParser ep = _eParsers.get(Integer.valueOf(msgType));
							if (ep != null)
								msg = ep.parse(cmdE, env.getOwner());
							else
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