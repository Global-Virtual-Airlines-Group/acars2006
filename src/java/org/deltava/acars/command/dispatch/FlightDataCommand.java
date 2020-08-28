// Copyright 2006, 2007, 2008, 2009, 2012, 2019, 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.dispatch;

import java.sql.Connection;

import org.deltava.beans.Pilot;

import org.deltava.dao.*;
import org.deltava.dao.acars.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.dispatch.FlightDataMessage;

/**
 * An ACARS server command to process Dispatch Messages.
 * @author Luke
 * @version 9.1
 * @since 1.1
 */

public class FlightDataCommand extends DispatchCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
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
		ACARSConnection dc = null;
		if (!isPlot) {
			dc = ctx.getACARSConnection(msg.getRecipient());
			if (dc == null) {
				ackMsg.setEntry("error", "Unknown recipient - " + msg.getRecipient());
				log.warn("Cannot send dispatch message to " + msg.getRecipient());
			}
		}

		boolean canCreate = usr.isInRole("Route");
		try {
			Connection c = ctx.getConnection();
			ctx.startTX();
			
			// Populate the gates
			GetGates gdao = new GetGates(c);
			if (msg.getGateD() != null)
				msg.setGateD(gdao.getGate(msg.getAirportD(), msg.getSimulator(), msg.getGateD().getName()));
			if (msg.getGateA() != null)
				msg.setGateA(gdao.getGate(msg.getAirportA(), msg.getSimulator(), msg.getGateA().getName()));
			
			// Log things
			if ((dc != null) && (msg.getLogID() == 0)) {
				SetDispatch dlwdao = new SetDispatch(c);
				dlwdao.writeLog(msg, dc.getUser());
			}

			// Save the dispatch message data
			if ((msg.getRouteID() == 0) && canCreate && !msg.getNoSave()) {
				GetACARSRoute rdao = new GetACARSRoute(c);
				int dupeID = rdao.hasDuplicate(msg, msg.getRoute());

				// Write the route
				if (dupeID == 0) {
					SetRoute dao = new SetRoute(c);
					dao.save(msg, con.getClientBuild());
				} else {
					log.warn(con.getUser().getName() + " attempting to save duplicate of Route #" + dupeID);
					msg.setRouteID(dupeID);
				}
			}
			
			ctx.commitTX();
		} catch (DAOException de) {
			ctx.rollbackTX();
			log.warn("Cannot save/update route data - " + de.getMessage(), de);
		} finally {
			ctx.release();
		}

		// Send out the dispatch data
		ackMsg.setEntry("routeID", String.valueOf(msg.getRouteID()));
		ackMsg.setEntry("logID", String.valueOf(msg.getLogID()));
		ackMsg.setEntry("msgs", (dc == null) ? "0" : "1");
		if (dc != null) {
			log.info("Dispatch info from " + usr.getPilotCode() + " to " + dc.getUserID());
			ctx.push(msg, dc.getID(), false);
		}

		ctx.push(ackMsg);
	}
}