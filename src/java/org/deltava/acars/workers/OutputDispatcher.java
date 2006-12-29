// Copyright 2004, 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.Iterator;

import org.deltava.acars.beans.Envelope;
import org.deltava.acars.beans.MessageStack;
import org.deltava.acars.message.Message;

import org.deltava.acars.xml.*;
import org.deltava.beans.acars.ServerStats;

/**
 * An ACARS Server worker to generate XML messages and dispatch them to the proper connection.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public final class OutputDispatcher extends Worker {

	/**
	 * Initializes the Worker.
	 */
	public OutputDispatcher() {
		super("Output Dispatcher", OutputDispatcher.class);
	}

	/**
	 * Executes the Thread.
	 */
	public void run() {
		log.info("Started");

		while (!Thread.currentThread().isInterrupted()) {
			_status.execute();

			// Translate and dispatch the messages on the bean output stack
			while (MessageStack.MSG_OUTPUT.hasNext()) {
				Envelope env = MessageStack.MSG_OUTPUT.pop();
				if (env != null) {
					log.debug("Dispatching message to " + env.getOwnerID());
					_status.setMessage("Dispatching message to " + env.getOwnerID());

					Message msg = (Message) env.getMessage();
					try {
						MessageWriter.dispatch(env.getConnectionID(), msg);
					} catch (XMLException xe) {
						log.error("Cannot dispatch - " + xe.getMessage());
					}
				}
			}

			// Dump the messages to the output stack
			if (MessageWriter.hasMessages()) {
				_status.setMessage("Pushing messages to XML Output Stack");
				synchronized (MessageWriter.class) {
					for (Iterator<Envelope> i = MessageWriter.getMessages().iterator(); i.hasNext();) {
						Envelope env = i.next();
						MessageStack.RAW_OUTPUT.push(env);
						ServerStats.msgOut(String.valueOf(env.getMessage()).length());
					}

					// Reset the message writer's internal documents
					MessageWriter.reset();
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