// Copyright 2006, 2007, 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.dispatch;

import java.util.*;

import org.deltava.beans.Pilot;

import org.deltava.dao.*;
import org.deltava.dao.acars.SetRoute;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.dispatch.FlightDataMessage;

/**
 * An ACARS server command to process Dispatch Messages.
 * @author Luke
 * @version 2.2
 * @since 1.1
 */

public class FlightDataCommand extends DispatchCommand {
	
	/**
	 * Initializes the Command.
	 */
	public FlightDataCommand() {
		super(FlightDataCommand.class);
	}
	
	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the inbound message and the owner
		Pilot usr = env.getOwner();
		FlightDataMessage msg = (FlightDataMessage) env.getMessage();
		ACARSConnection con = ctx.getACARSConnection();
		if ((usr == null) || (con == null) || (!con.getIsDispatch())) {
			log.warn("Unauthorized dispatch message from " + env.getConnectionID());
			return;
		}
		
		// Create the ACK message
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(usr, msg.getID());
		
		// Check if this is saving a route
		boolean isPlot = (msg.getRecipient() != null) && (msg.getRecipient().startsWith("$"));

		// Get the recipient
		Collection<ACARSConnection> dstC = new ArrayList<ACARSConnection>();
		if (!isPlot) {
			dstC.addAll(ctx.getACARSConnections(msg.getRecipient()));
			if (dstC.isEmpty() && !isPlot) {
				ackMsg.setEntry("error", "Unknown recipient - " + msg.getRecipient());
				log.warn("Cannot send dispatch message to " + msg.getRecipient());
			}
		}
		
		// Save the dispatch message data
		boolean canCreate = usr.isInRole("Route");
		if ((msg.getRouteID() == 0) && canCreate && !msg.getNoSave()) {
			try {
				SetRoute dao = new SetRoute(ctx.getConnection());
				dao.save(msg, con.getClientVersion());
			} catch (DAOException de) {
				log.warn("Cannot save/update route data - " + de.getMessage(), de);
			} finally {
				ctx.release();
			}
		}
		
		// Send out the dispatch data
		ackMsg.setEntry("routeID", String.valueOf(msg.getRouteID()));
		ackMsg.setEntry("msgs", String.valueOf(dstC.size()));
		if (!isPlot) {
			for (Iterator<ACARSConnection> i = dstC.iterator(); i.hasNext(); ) {
				ACARSConnection ac = i.next();
				log.info("Dispatch info from " + usr.getPilotCode() + " to " + ac.getUserID());
				ctx.push(msg, ac.getID());
			}
		}
		
		// Send out the ack
		ctx.push(ackMsg, ctx.getACARSConnection().getID());
	}
}