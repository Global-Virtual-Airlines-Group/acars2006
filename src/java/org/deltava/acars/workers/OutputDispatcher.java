// Copyright 2004, 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.*;

import org.jdom.*;
import org.jdom.output.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.Message;

import org.deltava.acars.xml.*;

import org.deltava.beans.Pilot;
import org.deltava.beans.acars.ServerStats;

import org.deltava.util.system.SystemData;

/**
 * An ACARS Server worker to generate XML messages and dispatch them to the proper connection.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public final class OutputDispatcher extends Worker {
	
	private final Map<Integer, MessageFormatter> _formatters = new HashMap<Integer, MessageFormatter>();
	private final XMLOutputter _xmlOut = new XMLOutputter(Format.getPrettyFormat().setEncoding("ISO-8859-1"));
	
	/**
	 * Initializes the Worker.
	 */
	public OutputDispatcher() {
		super("Output Dispatcher", OutputDispatcher.class);
	}
	
	/**
	 * Initializes the Worker.
	 * @see Worker#open()
	 */
	@SuppressWarnings("unchecked")
	public void open() {
		super.open();
		
		// Build the message formatter map
		Map<String, String> versions = (Map) SystemData.getObject("acars.protocols");
		if (versions == null) {
			log.warn("No trasnalation packages specified!");
			versions = Collections.emptyMap();
		}
		
		// Initialize the formatters
		for (Iterator<String> i = versions.keySet().iterator(); i.hasNext(); ) {
			String version = i.next();
			String pkg = versions.get(version);
			try {
				Class pClass = Class.forName(pkg + ".Formatter");
				_formatters.put(new Integer(version.substring(1)), (MessageFormatter) pClass.newInstance());
			} catch (Exception e) {
				log.error("Error loading " + version + " Message Formatter", e);
			}
		}
	}

	/**
	 * Executes the Thread.
	 */
	public void run() {
		log.info("Started");
		_status.setStatus(WorkerStatus.STATUS_START);

		while (!Thread.currentThread().isInterrupted()) {
			_status.execute();

			// Translate and dispatch the messages on the bean output stack
			Map<Long, Pilot> users = new HashMap<Long, Pilot>();
			Map<Long, Document> docs = new HashMap<Long, Document>();
			while (MessageStack.MSG_OUTPUT.hasNext()) {
				MessageEnvelope env = MessageStack.MSG_OUTPUT.pop();
				if (env != null) {
					Message msg = env.getMessage();
					log.debug("Dispatching message to " + env.getOwnerID());
					_status.setMessage("Dispatching message to " + env.getOwnerID());
					
					// Determine the protocol version for each message
					ACARSConnection ac = _pool.get(env.getConnectionID());

					// Get the XML document, if none exists create it
					if (ac != null) {
						Document doc = docs.get(new Long(env.getConnectionID()));
						if (doc == null) {
							Element e = new Element(ProtocolInfo.RSP_ELEMENT_NAME);
							e.setAttribute("version", String.valueOf(ac.getProtocolVersion()));
							doc = new Document(e);
							docs.put(new Long(env.getConnectionID()), doc);
							users.put(new Long(env.getConnectionID()), ac.getUser());
						}
						
						// Get the formatter
						MessageFormatter fmt = _formatters.get(new Integer(ac.getProtocolVersion()));
						if (fmt == null)
							log.warn("No formatter found for protocol v" + ac.getProtocolVersion());
						else {
							try {
								Element msgE = fmt.format(msg);
								if (msgE == null)
									return;
							
								// Add the element to the XML document's root element
								Element root = doc.getRootElement();
								root.addContent(msgE);
							} catch (XMLException xe) {
								log.error("Cannot dispatch - " + xe.getMessage());
							}
						}
					}
				}
			}

			// Dump the messages to the output stack
			if (!docs.isEmpty()) {
				_status.setMessage("Pushing messages to XML Output Stack");
				for (Iterator<Long> i = docs.keySet().iterator(); i.hasNext();) {
					Long conID = i.next();
					Document doc = docs.get(conID);
					Pilot user = users.get(conID);
					
					// Convert the document to text
					String xml = _xmlOut.outputString(doc);

					// Push to the output stack
					MessageStack.RAW_OUTPUT.push(new TextEnvelope(user, xml, conID.longValue()));
					ServerStats.msgOut(xml.length());
				}

				// Wake up a single thread waiting for something on the formatted input stack, or multiple if multiple
				// messages waiting
				MessageStack.RAW_OUTPUT.wakeup((MessageStack.RAW_OUTPUT.size() > 1));
			}

			// Log execution
			_status.complete();
			_status.setMessage("Idle");

			// Wait until something is on the bean output stack
			MessageStack.MSG_OUTPUT.waitForActivity();
		}
	}
}