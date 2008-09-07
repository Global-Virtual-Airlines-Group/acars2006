// Copyright 2007, 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.dispatch;

import java.util.*;
import java.sql.Connection;

import org.deltava.beans.Pilot;
import org.deltava.beans.acars.RoutePlan;

import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.message.AcknowledgeMessage;
import org.deltava.acars.message.dispatch.*;

import org.deltava.acars.command.*;

import org.deltava.dao.*;

/**
 * An ACARS Command to load flight routes.
 * @author Luke
 * @version 2.2
 * @since 2.0
 */

public class RouteRequestCommand extends DispatchCommand {

	/**
	 * Initializes the Command.
	 */
	public RouteRequestCommand() {
		super(RouteRequestCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the inbound message and the owner
		Pilot usr = env.getOwner();
		RouteRequestMessage msg = (RouteRequestMessage) env.getMessage();

		try {
			RouteInfoMessage rmsg = new RouteInfoMessage(usr, msg.getID());
			Connection con = ctx.getConnection();
			
			// Load the routes
			GetACARSRoute rdao = new GetACARSRoute(con);
			Collection<RoutePlan> plans = rdao.getRoutes(msg.getAirportD(), msg.getAirportA());
			for (RoutePlan rp : plans)
				rmsg.addPlan(rp);
			
			// Send the response
			rmsg.setMessage("Loaded " + plans.size() + " Dispatch routes from database");
			ctx.push(rmsg, env.getConnectionID());
		} catch (DAOException de) {
			log.error("Cannot load route data - " + de.getMessage(), de);
			AcknowledgeMessage errorMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			errorMsg.setEntry("error", "Cannot load route data");
			ctx.push(errorMsg, env.getConnectionID());
		} finally {
			ctx.release();
		}
	}
}