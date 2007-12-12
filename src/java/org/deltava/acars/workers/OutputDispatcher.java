// Copyright 2004, 2005, 2006, 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.*;

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
 * @version 2.0
 * @since 1.0
 */

public final class OutputDispatcher extends Worker {

	private final Map<Integer, MessageFormatter> _formatters = new HashMap<Integer, MessageFormatter>();
	private final XMLOutputter _xmlOut = new XMLOutputter(Format.getPrettyFormat().setEncoding("UTF-8"));

	private class DatedDocument extends Document {

		private long _time = Long.MAX_VALUE;

		DatedDocument(Element el) {
			super(el);
		}

		public long getTime() {
			return _time;
		}

		public void setTime(long time) {
			if ((time < _time) && (time > 0))
				_time = time;
		}
	}

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
	public void open() {
		super.open();

		// Build the message formatter map
		Map versions = (Map) SystemData.getObject("acars.protocols");
		if (versions == null) {
			log.warn("No trasnalation packages specified!");
			versions = Collections.emptyMap();
		}

		// Initialize the formatters
		for (Iterator i = versions.keySet().iterator(); i.hasNext();) {
			String version = (String) i.next();
			String pkg = (String) versions.get(version);
			try {
				Class pClass = Class.forName(pkg + ".format.Formatter");
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

		while (!Thread.currentThread().isInterrupted()) {
			_status.setMessage("Idle");
			Map<Long, Pilot> users = new HashMap<Long, Pilot>();
			Map<Long, DatedDocument> docs = new HashMap<Long, DatedDocument>();

			try {
				MessageEnvelope env = MSG_OUTPUT.take();
				_status.execute();

				// Translate and dispatch the messages on the bean output stack
				while (env != null) {
					Message msg = env.getMessage();
					_status.setMessage("Dispatching message to " + env.getOwnerID());
					if (log.isDebugEnabled())
						log.debug("Dispatching message to " + env.getOwnerID());

					// Determine the protocol version for each message
					ACARSConnection ac = _pool.get(env.getConnectionID());

					// Get the XML document, if none exists create it
					if (ac != null) {
						DatedDocument doc = docs.get(new Long(env.getConnectionID()));
						if (doc == null) {
							Element e = new Element(ProtocolInfo.RSP_ELEMENT_NAME);
							e.setAttribute("version", String.valueOf(ac.getProtocolVersion()));
							doc = new DatedDocument(e);
							docs.put(new Long(env.getConnectionID()), doc);
							users.put(new Long(env.getConnectionID()), ac.getUser());
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
							if (msgE == null)
								return;

							// Add the element to the XML document's root element
							Element root = doc.getRootElement();
							root.addContent(msgE);
							doc.setTime(msg.getTime());
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
					for (Iterator<Long> i = docs.keySet().iterator(); i.hasNext();) {
						Long conID = i.next();
						DatedDocument doc = docs.get(conID);
						Pilot user = users.get(conID);

						// Convert the document to text
						String xml = _xmlOut.outputString(doc);

						// Push to the output stack
						TextEnvelope outenv = new TextEnvelope(user, xml, conID.longValue());
						outenv.setTime(doc.getTime());
						RAW_OUTPUT.add(outenv);
					}
				}
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}

			// Log execution
			_status.complete();
		}
		
		log.warn("Shutting Down");
	}
}