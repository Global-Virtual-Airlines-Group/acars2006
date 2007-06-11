// Copyright 2004, 2005, 2006, 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.Message;
import org.deltava.acars.xml.MessageParser;

import org.deltava.beans.acars.ServerStats;

import org.deltava.util.system.SystemData;

/**
 * An ACARS Worker to translate XML messages into Java objects.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public final class InputTranslator extends Worker {
	
	private final Map<Integer, MessageParser> _parsers = new HashMap<Integer, MessageParser>();

	/**
	 * Initializes the Worker.
	 */
	public InputTranslator() {
		super("Input Stack Processor", InputTranslator.class);
	}
	
	/**
	 * Initializes the Worker.
	 * @see Worker#open()
	 */
	@SuppressWarnings("unchecked")
	public void open() {
		super.open();
		Map<String, String> versions = (Map) SystemData.getObject("acars.protocols");
		if (versions == null) {
			log.warn("No trasnalation packages specified!");
			versions = Collections.emptyMap();
		}
		
		// Initialize the parsers
		for (Iterator<String> i = versions.keySet().iterator(); i.hasNext(); ) {
			String version = i.next();
			String pkg = versions.get(version);
			try {
				Class pClass = Class.forName(pkg + ".Parser");
				_parsers.put(new Integer(version.substring(1)), (MessageParser) pClass.newInstance());
			} catch (Exception e) {
				log.error("Error loading " + version + " Message Parser", e);
			}
		}
	}

	/**
	 * Executes the thread.
	 */
	public void run() {
		log.info("Started");
		_status.setStatus(WorkerStatus.STATUS_START);
		
		while (!Thread.currentThread().isInterrupted()) {
			_status.setMessage("Idle");
			try {
				TextEnvelope env = RAW_INPUT.take();
				_status.execute();
				_status.setMessage("Translating Message from " + env.getOwnerID());
				log.debug("Message received from " + env.getOwnerID());
				
				// Get the proper message parser
				MessageParser parser = _parsers.get(new Integer(env.getVersion()));
				try {
					Collection<Message> msgs = parser.parse(env);
					for (Iterator<Message> i = msgs.iterator(); i.hasNext();) {
						Message msg = i.next();
						if (msg.getType() != Message.MSG_QUIT)
							MSG_INPUT.add(new MessageEnvelope(msg, env.getConnectionID()));
					}
					
					// Log the messages
					ServerStats.msgIn(String.valueOf(env.getMessage()).length()); 
				} catch (Exception e) {
					log.warn("Translation Error - " + e.getMessage(), e);
				}
				
				// Log execution
				_status.complete();
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
		}
	}
}