// Copyright 2010, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.mp.*;

import org.gvagroup.ipc.WorkerStatus;

/**
 * An ACARS worker thread to aggregate multi-player update messages.
 * @author Luke
 * @version 6.4
 * @since 3.0
 */

public class MPAggregator extends Worker {

	/**
	 * Initializes the Worker.
	 */
	public MPAggregator() {
		super("MP Aggregator", 50, MPAggregator.class);
	}

	/**
	 * Executes the Thread.
	 */
	@Override
	public void run() {
		log.info("Started");
		_status.setStatus(WorkerStatus.STATUS_START);
		
		final Map<Long, MPUpdateMessage> upds = new HashMap<Long, MPUpdateMessage>();
		while (!Thread.currentThread().isInterrupted()) {
			_status.setMessage("Idle");
			try {
				MessageEnvelope env = MP_UPDATE.poll(30, TimeUnit.SECONDS);
				_status.execute(); _status.setMessage("Aggregating Updates");

				// Get all of the entries
				while (env != null) {
					MPUpdateMessage msg = (MPUpdateMessage) env.getMessage();
					Long cid = Long.valueOf(env.getConnectionID());
					
					// Get the aggregated message
					MPUpdateMessage mpmsg = upds.get(cid);
					if (mpmsg == null) {
						mpmsg = new MPUpdateMessage(false);
						upds.put(cid, mpmsg);
					}
						
					// Add the update
					for (MPUpdate upd : msg.getUpdates()) {
						if (!mpmsg.add(upd))
							log.warn("Duplicate MP update attempted!");
					}
					
					env = MP_UPDATE.poll();
				}
				
				// Dump the messages out
				upds.entrySet().forEach(me -> MSG_OUTPUT.add(new MessageEnvelope(me.getValue(), me.getKey().longValue())));
				
				// Sleep for a bit to let them accumulate
				_status.setMessage("Pausing for Updates");
				Thread.sleep(250);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			} finally {
				upds.clear();
			}
			
			_status.complete();
		}
	}
}