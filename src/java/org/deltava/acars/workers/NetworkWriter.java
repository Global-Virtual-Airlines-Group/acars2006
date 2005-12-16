// Copyright (c) 2004, 2005 Global Virtual Airline Group. All Rights Reserved.
package org.deltava.acars.workers;

import org.deltava.acars.beans.*;

import org.deltava.util.system.SystemData;

/**
 * An ACARS Server task to handle writing to network connections.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class NetworkWriter extends Worker {

	private ACARSConnectionPool _pool;

	/**
	 * Initializes the worker task.
	 * @param threadID the thread ID
	 */
	public NetworkWriter(int threadID) {
		super("Network I/O Writer #" + threadID, NetworkWriter.class.getName() + "-" + threadID);
	}

	public final void open() {
		super.open();
		_pool = (ACARSConnectionPool) SystemData.getObject(SystemData.ACARS_POOL);
	}

	/*
	 * (non-Javadoc)
	 * @see org.deltava.acars.workers.Worker#$run0()
	 */
	@Override
	protected void $run0() throws Exception {
		log.info("Started");

		while (!Thread.currentThread().isInterrupted()) {
			// Loop through the raw output stack
			while (MessageStack.RAW_OUTPUT.hasNext()) {
				Envelope env = MessageStack.RAW_OUTPUT.pop();

				// Get the connection and write the message
				ACARSConnection c = _pool.get(env.getConnectionID());
				if (c != null) {
					log.debug("Writing to " + c.getRemoteAddr());
					_status.setMessage("Writing to " + c.getUserID() + " (" + c.getRemoteAddr() + ")");
					c.write((String) env.getMessage());
				}
			}

			// Log execution
			_status.execute();
			_status.setMessage("Idle");
			
			// Wait until something is on the bean output stack or we get interrupted
			try {
				MessageStack.RAW_OUTPUT.waitForActivity();
			} catch (InterruptedException ie) {
				log.info("Interrupted");
				Thread.currentThread().interrupt();
			}
		}
		
		// Mark the interrupt
		log.info("Interrupted");
	}
}