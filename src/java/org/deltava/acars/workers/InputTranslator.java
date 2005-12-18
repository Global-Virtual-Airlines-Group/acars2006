// Copyright 2005 Luke J. Kolin. All Rights Resever.
package org.deltava.acars.workers;

import java.util.Iterator;

import org.deltava.acars.beans.Envelope;
import org.deltava.acars.beans.MessageStack;

import org.deltava.acars.message.Message;
import org.deltava.acars.xml.MessageReader;

import org.deltava.beans.acars.ServerStats;

/**
 * An ACARS Worker to translate XML messages into Java objects.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public final class InputTranslator extends Worker {

	public InputTranslator() {
		super("Input Stack Processor", InputTranslator.class);
	}

	protected void $run0() {
		log.info("Started");

		while (!Thread.currentThread().isInterrupted()) {
			// Process stuff on the input stack
			while (MessageStack.RAW_INPUT.hasNext()) {
				Envelope env = MessageStack.RAW_INPUT.pop();
				if (env != null) {
					_status.setMessage("Translating Message from " + env.getOwnerID());
					log.debug("Message received from " + env.getOwnerID());
					try {
						MessageReader reader = new MessageReader(env);

						// Copy the messages from the parser to the formatted input stack
						for (Iterator<Message> i = reader.parse().iterator(); i.hasNext();) {
							Message msg = i.next();
							if (msg.getType() != Message.MSG_QUIT)
								MessageStack.MSG_INPUT.push(new Envelope(msg, env.getConnectionID()));
						}
						
						// Log the messages
						ServerStats.msgIn(String.valueOf(env.getMessage()).length()); 
					} catch (Exception e) {
						log.warn("Translation Error - " + e.getMessage(), e);
					}
				}
			}

			// Wake up a single thread waiting for something on the formatted input stack, or multiple if multiple messages waiting
			if (MessageStack.MSG_INPUT.size() == 1) {
				synchronized (MessageStack.MSG_INPUT) {
					MessageStack.MSG_INPUT.notify();
				}
			} else {
				MessageStack.MSG_INPUT.wakeup();
			}

			// Log execution
			_status.execute();
			_status.setMessage("Idle");

			// Wait until something is on the input stack or we get interrupted
			try {
				MessageStack.RAW_INPUT.waitForActivity();
			} catch (InterruptedException ie) {
				log.info("Interrupted");
				Thread.currentThread().interrupt();
			}
		}
	}
}