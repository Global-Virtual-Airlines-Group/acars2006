// Copyright 2008, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;

import org.deltava.beans.schedule.*;
import org.deltava.dao.*;
import org.deltava.util.*;

import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.OceanicTrackMessage;

/**
 * An ACARS data command to return available NAT and PACOT data.
 * @author Luke
 * @version 2.6
 * @since 2.2
 */

public class OceanicTrackCommand extends DataCommand {
	
	/**
	 * Initializes the Command.
	 */
	public OceanicTrackCommand() {
		super(OceanicTrackCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message and the route type
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		String routeType = msg.hasFlag("type") ? msg.getFlag("type") : "NAT"; 
		int rType = StringUtils.arrayIndexOf(OceanicRoute.TYPES, routeType.toUpperCase(), OceanicRoute.NAT);
		
		// Get the response and add the Concorde tracks if pulling down NATs
		OceanicTrackMessage rspMsg = new OceanicTrackMessage(env.getOwner(), msg.getID());
		if (rType == OceanicRoute.NAT) {
			for (Iterator<? extends OceanicWaypoints> i = OceanicWaypoints.CONC_ROUTES.iterator(); i.hasNext(); ) {
				OceanicWaypoints route = i.next();
				rspMsg.add(route);
			}
		}
		
		// Get the date
		Date dt = msg.hasFlag("date") ? StringUtils.parseDate(msg.getFlag("date"), "MM/dd/yyyy") : null;
		try {
			GetRoute dao = new GetRoute(ctx.getConnection());
			rspMsg.addAll(dao.getOceanicTrakcs(rType, dt).values());
		} catch (DAOException de) {
			String trackType = OceanicRoute.TYPES[rType];
			log.error("Error loading " + trackType + " tracks for " + msg.getFlag("date") + " - " + de.getMessage(), de);
			AcknowledgeMessage errMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			errMsg.setEntry("error", "Cannot load " + msg.getFlag("date") + " " + trackType + " data");
			ctx.push(errMsg, ctx.getACARSConnection().getID());
		} finally {
			ctx.release();
		}
		
		// Push the message
		ctx.push(rspMsg, env.getConnectionID());
	}
}