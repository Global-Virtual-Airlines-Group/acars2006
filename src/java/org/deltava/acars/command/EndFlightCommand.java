// Copyright 2005, 2006, 2007, 2008, 2009, 2011, 2012, 2016, 2019, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.time.Instant;

import org.apache.logging.log4j.*;

import static org.deltava.acars.workers.Worker.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.mp.RemoveMessage;

import org.deltava.dao.acars.SetInfo;
import org.deltava.dao.DAOException;

/**
 * An ACARS Command to log the completion of a flight.
 * @author Luke
 * @version 11.0
 * @since 1.0
 */

public class EndFlightCommand extends ACARSCommand {

	private static final Logger log = LogManager.getLogger(EndFlightCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the message and the connection
		EndFlightMessage msg = (EndFlightMessage) env.getMessage();
		ACARSConnection con = ctx.getACARSConnection();
		if ((con == null) || con.getIsDispatch())
			return;
		
		// Create the ack message
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());

		// Get the current info
		InfoMessage iMsg = con.getFlightInfo();
		if (iMsg == null) {
			log.warn("No Flight Information for " + con.getUserID());
			ackMsg.setEntry("sendInfo", "true");
			ctx.push(ackMsg);
			return;
		}
		
		// If the flight was already ended, then just send the ACK and abort
		if ((iMsg.getEndTime() != null) && (iMsg.getLivery() == null)) {
			ctx.push(ackMsg);
			return;
		}
		
		// Save an MPRemove message if we are an MP connection
		if (iMsg.getLivery() != null) {
			iMsg.setLivery(null);
			RemoveMessage mrmsg = new RemoveMessage(con.getUser(), iMsg.getFlightID());
			MSG_INPUT.add(new MessageEnvelope(mrmsg, con.getID()));
		}

		// Write the info to the database
		iMsg.setComplete(true);
		iMsg.setEndTime(Instant.now());
		try {
			SetInfo infoDAO = new SetInfo(ctx.getConnection());
			infoDAO.close(iMsg.getFlightID(), true);
		} catch (DAOException de) {
			log.error(de.getMessage(), de);
		} finally {
			ctx.release();
		}
		
		// Clear flight info and log
		log.info("Flight Completed by " + con.getUserID());
		con.setPosition(null);
		ctx.push(ackMsg);
	}
}