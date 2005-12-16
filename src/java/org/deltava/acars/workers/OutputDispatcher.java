package org.deltava.acars.workers;

import java.util.Iterator;

import org.deltava.acars.beans.Envelope;
import org.deltava.acars.beans.MessageStack;
import org.deltava.acars.message.Message;

import org.deltava.acars.xml.*;
import org.deltava.beans.acars.ServerStats;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public final class OutputDispatcher extends Worker {

	public OutputDispatcher() {
		super("Output Dispatcher", OutputDispatcher.class);
	}

	protected void $run0() {
		log.info("Started");

		while (!Thread.currentThread().isInterrupted()) {
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
				for (Iterator<Envelope> i = MessageWriter.getMessages().iterator(); i.hasNext(); ) {
					Envelope env = i.next();
					MessageStack.RAW_OUTPUT.push(env);
					ServerStats.msgOut(String.valueOf(env.getMessage()).length());
				}

				// Reset the message writer's internal documents
				MessageWriter.reset();
			}
			
			// Wake up a single thread waiting for something on the raw output stack
			synchronized (MessageStack.RAW_OUTPUT) {
				MessageStack.RAW_OUTPUT.notify();
			}

			// Log execution
			_status.execute();
			_status.setMessage("Idle");

			// Wait until something is on the bean output stack or we get interrupted
			try {
				MessageStack.MSG_OUTPUT.waitForActivity();
			} catch (InterruptedException ie) {
				log.info("Interrupted");
				Thread.currentThread().interrupt();
			}
		}
	}
}