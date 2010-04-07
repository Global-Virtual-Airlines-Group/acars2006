// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.jdom.*;
import org.jdom.output.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.Message;

import org.deltava.acars.xml.*;

import org.deltava.beans.Pilot;

import org.deltava.util.system.SystemData;

/**
 * An ACARS Server worker to generate XML messages and dispatch them to the proper connection.
 * @author Luke
 * @version 3.0
 * @since 1.0
 */

public final class OutputDispatcher extends Worker {

	private final Map<Integer, MessageFormatter> _formatters = new HashMap<Integer, MessageFormatter>();
	
	private final XMLOutputter _xmlOut = new XMLOutputter(Format.getPrettyFormat().setEncoding("UTF-8"));
	private final XMLOutputter _tinyOut = new XMLOutputter(Format.getCompactFormat().setEncoding("UTF-8"));

	private static final class DatedDocument extends Document {

		private boolean _isCompact;
		private long _time = Long.MAX_VALUE;

		DatedDocument(Element el) {
			super(el);
		}
		
		boolean isCompact() {
			return _isCompact;
		}
		
		boolean isEmpty() {
			Element r = getRootElement();
			return (r == null) || (r.getChildren().isEmpty());
		}

		long getTime() {
			return _time;
		}
		
		public void setCompact(boolean isCompact) {
			_isCompact = isCompact;
		}

		public void setTime(long time) {
			_time = Math.max(0, Math.min(_time, time));
		}
	}

	/**
	 * Initializes the Worker.
	 */
	public OutputDispatcher() {
		super("Output Dispatcher", 60, OutputDispatcher.class);
	}

	/**
	 * Initializes the Worker.
	 * @see Worker#open()
	 */
	public void open() {
		super.open();

		// Build the message formatter map
		Map<?, ?> versions = (Map<?, ?>) SystemData.getObject("acars.protocols");
		if (versions == null) {
			log.warn("No trasnalation packages specified!");
			versions = Collections.emptyMap();
		}

		// Initialize the formatters
		for (Iterator<?> i = versions.keySet().iterator(); i.hasNext();) {
			String version = (String) i.next();
			String pkg = (String) versions.get(version);
			try {
				Class<?> pClass = Class.forName(pkg + ".format.Formatter");
				_formatters.put(Integer.valueOf(version.substring(1)), (MessageFormatter) pClass.newInstance());
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

		final Map<Long, Pilot> users = new HashMap<Long, Pilot>();
		final Map<Long, DatedDocument> docs = new HashMap<Long, DatedDocument>();
		while (!Thread.currentThread().isInterrupted()) {
			_status.setMessage("Idle");

			try {
				MessageEnvelope env = MSG_OUTPUT.poll(30, TimeUnit.SECONDS);
				_status.execute();

				// Translate and dispatch the messages on the bean output stack
				while (env != null) {
					Message msg = env.getMessage();
					_status.setMessage("Dispatching message to " + env.getOwnerID());
					if (log.isDebugEnabled())
						log.debug("Dispatching message to " + env.getOwnerID());

					// Determine the protocol version for each message
					Long cid = Long.valueOf(env.getConnectionID());
					ACARSConnection ac = _pool.get(cid);

					// Get the XML document, if none exists create it
					if (ac != null) {
						DatedDocument doc = docs.get(cid);
						if (doc == null) {
							Element e = new Element(ProtocolInfo.RSP_ELEMENT_NAME);
							e.setAttribute("version", String.valueOf(ac.getProtocolVersion()));
							doc = new DatedDocument(e);
							docs.put(cid, doc);
							users.put(cid, ac.getUser());
							doc.setCompact(false);
						}

						// Get the formatter
						MessageFormatter fmt = _formatters.get(Integer.valueOf(ac.getProtocolVersion()));
						if (fmt == null) {
							SortedSet<Integer> vers = new TreeSet<Integer>(_formatters.keySet());
							Integer defaultVersion = vers.first();
							log.warn("No formatter found for protocol v" + ac.getProtocolVersion() + " - using v" + defaultVersion);
							fmt = _formatters.get(defaultVersion);
						}

						try {
							Element msgE = fmt.format(msg);
							
							// Add the element to the XML document's root element
							if (msgE != null) {
								Element root = doc.getRootElement();
								root.addContent(msgE);
								doc.setTime(msg.getTime());
							}
						} catch (Exception e) {
							log.error("Cannot dispatch - " + e.getMessage(), e);
						}
					}

					// Check for another message
					env = MSG_OUTPUT.poll();
				}

				// Dump the messages to the output stack
				if (!docs.isEmpty()) {
					_status.setMessage("Pushing messages to XML Output Stack");
					for (Iterator<Map.Entry<Long, DatedDocument>> i = docs.entrySet().iterator(); i.hasNext();) {
						Map.Entry<Long, DatedDocument> me = i.next();
						Long conID = me.getKey();
						DatedDocument doc = me.getValue();
						Pilot user = users.get(conID);

						// Convert the document to text
						if (!doc.isEmpty()) {
							XMLOutputter out = doc.isCompact() ? _tinyOut : _xmlOut;
							String xml = out.outputString(doc);

							// Push to the output stack
							TextEnvelope outenv = new TextEnvelope(user, xml, conID.longValue());
							outenv.setTime(doc.getTime());
							RAW_OUTPUT.add(outenv);
						}
					}
				}
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			} finally {
				users.clear();
				docs.clear();
			}

			// Log execution
			_status.complete();
		}
		
		log.warn("Shutting Down");
	}
}