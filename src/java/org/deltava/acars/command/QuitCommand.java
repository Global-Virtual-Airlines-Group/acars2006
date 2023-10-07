// Copyright 2005, 2006, 2007, 2008, 2011, 2012, 2018, 2019, 2023 Global Virtual Airline Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;

import org.apache.logging.log4j.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.*;
import org.deltava.acars.message.dispatch.CancelMessage;

import org.deltava.dao.*;
import org.deltava.dao.acars.SetInfo;

/**
 * An ACARS command to handle disconnections by authenticated users.
 * @author Luke
 * @version 11.1
 * @since 1.0
 */

public class QuitCommand extends ACARSCommand {

	private static final Logger log = LogManager.getLogger(QuitCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the message
		QuitMessage msg = (QuitMessage) env.getMessage();

		// Mark the flight as closed
		if (msg.getFlightID() != 0) {
			try {
				SetInfo infoDAO = new SetInfo(ctx.getConnection());
				infoDAO.close(msg.getFlightID(), false);
			} catch (DAOException de) {
				log.error(de.getMessage(), de);
			} finally {
				ctx.release();
			}
		}

		// Create a deletepilots message
		PilotMessage drmsg = new PilotMessage(env.getOwner(), DataRequest.REMOVEUSER, msg.getID());
		drmsg.add(env.getOwner());
		drmsg.setDispatch(msg.isDispatch());

		// Get all connections
		Collection<ACARSConnection> cons = ctx.getACARSConnectionPool().getAll();
		if (msg.isHidden()) {
			for (ACARSConnection ac : cons) {
				if ((ac.getID() != env.getConnectionID()) && ac.isAuthenticated() && ac.getUser().isInRole("HR"))
					ctx.push(drmsg, ac.getID(), false);
			}
		} else {
			for (ACARSConnection ac : cons) {
				if (ac.getID() != env.getConnectionID()) ctx.push(drmsg, ac.getID(), false);
				if (msg.isDispatch() && (ac.getDispatcherID() == env.getConnectionID()))
					ctx.push(new CancelMessage(env.getOwner()), ac.getID(), false);
			}
		}
	}
}