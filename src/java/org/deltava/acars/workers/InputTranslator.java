// Copyright 2005 Luke J. Kolin. All Rights Resever.
package org.deltava.acars.workers;

import java.util.Iterator;

import org.deltava.acars.beans.Envelope;
import org.deltava.acars.message.Message;

import org.deltava.acars.xml.MessageReader;
import org.deltava.acars.xml.XMLException;

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
			while (_inStack.hasNext()) {
				Envelope env = _inStack.pop();
				if (env != null) {
					log.debug("Message received from " + env.getOwnerID());
					try {
						MessageReader reader = new MessageReader(env);

						// Copy the messages from the parser to the formatted input stack
						for (Iterator i = reader.parse().iterator(); i.hasNext();) {
							Message msg = (Message) i.next();
							if (msg.getType() != Message.MSG_QUIT)
								_outStack.push(new Envelope(msg, env.getConnectionID()));
						}
					} catch (XMLException xe) {
						log.error("Translation Error - " + xe.getMessage());
					}
				}
			}

			// Wake up any threads waiting for something on the output stack
			synchronized (_outStack) {
				_outStack.notifyAll();
			}

			// Wait until something is on the input stack or we get interrupted
			try {
				synchronized (_inStack) {
					_inStack.wait();
				}
			} catch (InterruptedException ie) {
				log.info("Interrupted");
				Thread.currentThread().interrupt();
			}
		}
	}
}