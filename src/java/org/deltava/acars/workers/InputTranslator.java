// Copyright 2004, 2005, 2006, 2007, 2008, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.Message;
import org.deltava.acars.xml.MessageParser;

import org.deltava.util.system.SystemData;

/**
 * An ACARS Worker to translate XML messages into Java objects.
 * @author Luke
 * @version 2.6
 * @since 1.0
 */

public final class InputTranslator extends Worker {
	
	private final Map<Integer, MessageParser> _parsers = new HashMap<Integer, MessageParser>();

	/**
	 * Initializes the Worker.
	 */
	public InputTranslator() {
		super("Input Translator", 30, InputTranslator.class);
	}
	
	/**
	 * Initializes the Worker.
	 * @see Worker#open()
	 */
	public void open() {
		super.open();
		Map<?, ?> versions = (Map<?, ?>) SystemData.getObject("acars.protocols");
		if (versions == null) {
			log.warn("No trasnalation packages specified!");
			versions = Collections.emptyMap();
		}
		
		// Initialize the parsers
		for (Iterator<?> i = versions.keySet().iterator(); i.hasNext(); ) {
			String version = (String) i.next();
			String pkg = (String) versions.get(version);
			try {
				Class<?> pClass = Class.forName(pkg + ".parse.Parser");
				_parsers.put(Integer.valueOf(version.substring(1)), (MessageParser) pClass.newInstance());
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
				TextEnvelope env = RAW_INPUT.poll(30, TimeUnit.SECONDS);
				_status.execute();
				while (env != null) {
					_status.setMessage("Translating Message from " + env.getOwnerID());
					if (log.isDebugEnabled())
						log.debug("Message received from " + env.getOwnerID());
				
					// Get the proper message parser
					MessageParser parser = _parsers.get(Integer.valueOf(env.getVersion()));
					try {
						Collection<Message> msgs = parser.parse(env);
						for (Iterator<Message> i = msgs.iterator(); i.hasNext();) {
							Message msg = i.next();
							if (msg.getType() != Message.MSG_QUIT)
								MSG_INPUT.add(new MessageEnvelope(msg, env.getConnectionID()));
						}
					} catch (Exception e) {
						log.warn("Translation Error - " + e.getMessage(), e);
					}
					
					env = RAW_INPUT.poll();
				}
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			
			// Log execution
			_status.complete();
		}
	}
}