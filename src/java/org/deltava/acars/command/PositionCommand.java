// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.Collection;
import java.util.concurrent.locks.*;

import org.apache.log4j.Logger;

import static org.deltava.acars.workers.Worker.MP_UPDATE;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.mp.MPUpdateMessage;

import org.deltava.dao.acars.SetPosition;

import org.deltava.util.CalendarUtils;
import org.deltava.util.system.SystemData;

import org.gvagroup.acars.ACARSFlags;

/**
 * An ACARS server command to process position updates.
 * @author Luke
 * @version 3.0
 * @since 1.0
 */

public class PositionCommand extends ACARSCommand {

	private static final Logger log = Logger.getLogger(PositionCommand.class);
	private static final ReentrantReadWriteLock _flushLock = new ReentrantReadWriteLock(true);
	private static final Lock w = _flushLock.writeLock();

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the Message and the ACARS Connection
		PositionMessage msg = (PositionMessage) env.getMessage();
		ACARSConnection ac = ctx.getACARSConnection();
		if (ac == null) {
			log.warn("Missing Connection for " + env.getOwnerID());
			return;
		} else if (ac.getIsDispatch()) {
			log.warn("Dispatch Client sending Position Report!");
			return;
		}

		// Create the ack message and envelope
		AcknowledgeMessage ackMsg = null;
		if (SystemData.getBoolean("acars.ack.position"))
			ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());

		// Get the last position report and its age
		InfoMessage info = ac.getFlightInfo();
		PositionMessage oldPM = ac.getPosition();
		if (info == null) {
			log.warn("No Flight Information for " + ac.getUserID());
			if (ackMsg != null)
				ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());

			ackMsg.setEntry("sendInfo", "true");
			ctx.push(ackMsg, env.getConnectionID());
			return;
		}
		
		// Adjust the message date
		msg.setDate(CalendarUtils.adjustMS(msg.getDate(), ac.getTimeOffset()));

		// Calculate the age of the last message
		long pmAge = System.currentTimeMillis() - ((oldPM == null) ? 0 : oldPM.getTime());

		// Queue it up
		if (msg.getNoFlood())
			SetPosition.queue(msg, ac.getFlightID());
		else {
			// Check for position flood
			if (pmAge >= SystemData.getInt("acars.position_interval")) {
				if (!msg.isFlagSet(ACARSFlags.FLAG_PAUSED)) {
					ac.setPosition(msg);
					if (msg.isLogged())
						SetPosition.queue(msg, ac.getFlightID());
				} else
					ac.setPosition(null);
			} else if (!msg.isLogged())
				ac.setPosition(msg);
			else
				log.warn("Position flood from " + ac.getUser().getName() + " (" + ac.getUserID() + "), interval=" + pmAge + "ms");
		}
		
		// Log message received
		ctx.push(ackMsg, env.getConnectionID());
		if (log.isDebugEnabled())
			log.debug("Received position from " + ac.getUserID());
		
		// Send it to any dispatchers that are around
		if ((ac.getFlightInfo().getNetwork() == null) && (!msg.getNoFlood())) {
			Collection<ACARSConnection> scopes = ctx.getACARSConnectionPool().getMP(msg);
			if (!scopes.isEmpty()) {
				MPUpdateMessage updmsg = new MPUpdateMessage(false);
				MPUpdate upd = new MPUpdate(ac.getUserData().getID(), msg);
				updmsg.add(upd);
			
				// Queue the message
				for (ACARSConnection rac : scopes)
					MP_UPDATE.add(new MessageEnvelope(updmsg, rac.getID()));
			}
		}

		// Check if the cache needs to be flushed
		if (w.tryLock()) {
			if (SetPosition.getMaxAge() > 32500) {
				try {
					SetPosition dao = new SetPosition(ctx.getConnection());
					int entries = dao.flush();
					log.info("Flushed " + entries + " cached position entries");
				} catch (Exception e) {
					log.error("Error flushing positions - " + e.getMessage(), e);
				} finally {
					ctx.release();
				}
			}
			
			// Release the lock
			while (_flushLock.isWriteLockedByCurrentThread())
				w.unlock();
		}
	}
}