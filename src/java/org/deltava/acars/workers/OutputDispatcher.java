// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.jdom2.*;
import org.jdom2.output.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.Message;
import org.deltava.acars.xml.*;
import org.deltava.beans.Pilot;

import org.deltava.util.system.SystemData;
import org.gvagroup.ipc.WorkerStatus;

/**
 * An ACARS Server worker to generate XML messages and dispatch them to the proper connection.
 * @author Luke
 * @version 5.1
 * @since 1.0
 */

public final class OutputDispatcher extends Worker {

	private final SortedMap<Integer, MessageFormatter> _formatters = new TreeMap<Integer, MessageFormatter>();
	private MessageFormatter _defaultFmt;
	
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
	@Override
	public void open() {
		super.open();

		// Build the message formatter map
		Map<?, ?> versions = (Map<?, ?>) SystemData.getObject("acars.protocols");
		if (versions == null)
			throw new IllegalStateException("No trasnalation packages specified");

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
		
		_defaultFmt = _formatters.get(_formatters.firstKey());
	}

	/**
	 * Executes the Thread.
	 */
	@Override
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
					ACARSConnection ac = _pool.get(env.getConnectionID());

					// Get the XML document, if none exists create it
					if (ac != null) {
						Long cid = Long.valueOf(env.getConnectionID());
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
							fmt = _defaultFmt;
							log.warn("No formatter found for protocol v" + ac.getProtocolVersion() + " - using v" + fmt.getProtocolVersion());
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

			_status.complete();
		}
		
		log.warn("Shutting Down");
	}
}