package org.deltava.acars.xml;

import java.util.*;

import org.jdom.*;
import org.jdom.output.*;

import org.apache.log4j.Logger;

import org.deltava.beans.Pilot;
import org.deltava.acars.beans.Envelope;
import org.deltava.acars.message.Message;

import org.deltava.util.IDGenerator;

/**
 * Converts message beans to XML.
 * @author Luke J. Kolin
 * @version 1.0
 * @since 1.0
 */

public class MessageWriter implements java.io.Serializable {
	
	private static final Logger log = Logger.getLogger(MessageWriter.class);
	
	private static final XMLOutputter _xmlOut = new XMLOutputter(Format.getPrettyFormat());

	// Keeps track of XML documents, formatters and users who have msgs waiting
	private static Map<Long, Document> _xdocs = new HashMap<Long, Document>();
	private static Map<Long, MessageFormatter> _formatters = new HashMap<Long, MessageFormatter>();
	private static Map<Long, Pilot> _users = new HashMap<Long, Pilot>();
	private static Set<Long> _dirty = new HashSet<Long>();

	// we're a singleton
	private MessageWriter() {
	}
	
	private static Document createXMLDoc(int pV) {
		
		// Create the root element
		Element e = new Element(ProtocolInfo.RSP_ELEMENT_NAME);
		e.setAttribute("version", String.valueOf(pV));
		
		// Return the new docuemnt
		return new Document(e);
	}

	public synchronized static void addConnection(long cid, Pilot userInfo, int pV) throws XMLException {
		
		// Check if the connection already exists
		if (contains(cid))
			throw new XMLException("Connection already registered");
		
		// Convert the CID into an object
		Long CID = new Long(cid);
		
		// Check if we already have the connection ID
		if (_xdocs.containsKey(CID))
			return;
			
		// Create a new Pilot bean and XML Document for the connection
		_xdocs.put(CID, createXMLDoc(pV));
		if (userInfo != null) _users.put(CID, userInfo);
		
		// Get the formatter for the protocol version
		try {
			String fmtClass = MessageWriter.class.getPackage().getName() + ".MessageFormatterV" + 
			String.valueOf(pV);
			Class fClass = Class.forName(fmtClass);
			MessageFormatter fmt = (MessageFormatter) fClass.newInstance();
			
			// Save the formatter
			_formatters.put(CID, fmt);
		} catch (Exception e) {
			throw new XMLException("Error creating formatter - " + e.getClass().getName(), e);
		}
	}
	
	public static boolean contains(long cid) {
		return _xdocs.containsKey(new Long(cid));
	}
	
	public static synchronized void reset() {
		for (Iterator i = _xdocs.keySet().iterator(); i.hasNext(); ) {
			Long CID = (Long) i.next();
			
			// Get the connection's message formatter to find the protocol to use
			MessageFormatter fmt = _formatters.get(CID); 
			_xdocs.put(CID, createXMLDoc(fmt.getProtocolVersion()));
		}
		
		// Clean the dirty connections
		_dirty.clear();
	}
	
	/**
	 * Removes a connection from the Message Writer.
	 * @param cid the connection ID
	 */
	public static synchronized void remove(long cid) {
		Long CID = new Long(cid);
		_xdocs.remove(CID);
		_formatters.remove(CID); 
		_dirty.remove(CID);
	}
	
	/**
	 * Send message to just a single connection.
	 * @param cid the Connection ID
	 * @param msg the Message
	 */  
	public static void dispatch(long cid, Message msg) throws XMLException {
		
		// Get the document that we want to add this message to
		Long CID = new Long(cid);
		Document doc = _xdocs.get(CID);
		if (doc == null)
			throw new XMLException("Connection " + Long.toHexString(cid).toUpperCase() + " disconnected");
			
		// Get the message formatter for that user
		MessageFormatter fmt = _formatters.get(CID);
		if (fmt == null)
			throw new XMLException("No Formatter for connection " + Long.toHexString(cid));
		
		// Format the message and turn it into an XML element
		Element msgE = fmt.format(msg);
		if (msgE == null)
			return;
		
		// Add the element to the XML document's root element
		Element root = doc.getRootElement();
		root.addContent(msgE);
		
		// Add to the list of dirty users
		_dirty.add(CID);
	}
	
	public static boolean hasMessages() {
		return !_dirty.isEmpty();
	}
	
	public static Collection<Envelope> getMessages() {
		
		// Get the list of connections with messages waiting
		List<Envelope> envs = new ArrayList<Envelope>();
		for (Iterator i = _dirty.iterator(); i.hasNext(); ) {
			Long CID = (Long) i.next();
			
			// Get the XML Document and pilot bean
			Document xdoc = _xdocs.get(CID);
			Pilot user = _users.get(CID);
			if (xdoc != null) {
				// Set the response message ID
				Element root = xdoc.getRootElement();
				root.setAttribute("id", Long.toHexString(IDGenerator.generate()).toUpperCase());
				
				// Create the envelope and save it
				String msgText = _xmlOut.outputString(xdoc);
				Envelope env = new Envelope(user, msgText, CID.longValue());
				envs.add(env);
				log.debug(msgText);
			}
		}
		
		// Return the envelopes
		return envs;
	}
}