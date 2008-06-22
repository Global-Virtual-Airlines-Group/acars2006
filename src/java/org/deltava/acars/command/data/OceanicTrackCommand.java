// Copyright 2008 Global Virtual Airlines Group. All Rights Reserved.
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
 * An ACARS data command to return available NAT data.
 * @author Luke
 * @version 2.2
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
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		OceanicTrackMessage rspMsg = new OceanicTrackMessage(env.getOwner(), msg.getID());
		for (Iterator<? extends OceanicWaypoints> i = OceanicWaypoints.CONC_ROUTES.iterator(); i.hasNext(); ) {
			OceanicWaypoints route = i.next();
			rspMsg.add(route);
		}
		
		// Get the date
		Date dt = msg.hasFlag("date") ? StringUtils.parseDate(msg.getFlag("date"), "MM/dd/yyyy") : null;
		try {
			GetRoute dao = new GetRoute(ctx.getConnection());
			rspMsg.addAll(dao.getOceanicTrakcs(OceanicRoute.NAT, dt).values());

		} catch (DAOException de) {
			log.error("Error loading NAT tracks for " + msg.getFlag("date") + " - " + de.getMessage(), de);
			AcknowledgeMessage errMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			errMsg.setEntry("error", "Cannot load " + msg.getFlag("date") + " NAT data");
			ctx.push(errMsg, ctx.getACARSConnection().getID());
		} finally {
			ctx.release();
		}
		
		// Push the message
		ctx.push(rspMsg, env.getConnectionID());
	}
}