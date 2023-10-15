// Copyright 2019, 2021, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import static org.deltava.acars.workers.Worker.MSG_INPUT;

import java.sql.Connection;

import org.apache.logging.log4j.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import org.deltava.beans.*;
import org.deltava.beans.acars.Restriction;

import org.deltava.dao.*;

import org.deltava.util.StringUtils;

/**
 * An ACARS server command to disconnect a user from the ACARS server.
 * @author Luke
 * @version 11.1
 * @since 8.7
 */

public class KickCommand extends ACARSCommand {
	
	private static final Logger log = LogManager.getLogger(KickCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message and check access
		KickMessage msg = (KickMessage) env.getMessage();
		if (!env.getOwner().isInRole("HR"))
			return;
		
		// Load the user
		ACARSConnection ac = ctx.getACARSConnection(msg.getRecipient());
		if (ac == null) {
			log.warn("Unknown user - {}", msg.getRecipient());
			ctx.push(new AcknowledgeMessage(ctx.getUser(), msg.getID(), "Unknown user - " + msg.getRecipient()));
			return;
		}
		
		// Tell the client not to reconnect
		ctx.push(msg, ac.getID(), true);
		
		// Save the QUIT message
		MSG_INPUT.add(new MessageEnvelope(new QuitMessage(ac), ac.getID()));
		
		// Send the ACK
		AcknowledgeMessage daMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
		daMsg.setEntry("cid", StringUtils.formatHex(ac.getID()));
		daMsg.setEntry("user", ac.getUserID());
		daMsg.setEntry("addr", ac.getRemoteAddr());
		ctx.push(daMsg);
		
		// Log the KICK
		StatusUpdate upd = new StatusUpdate(ac.getUser().getID(), UpdateType.COMMENT);
		upd.setAuthorID(ctx.getUser().getID());
		upd.setDescription((msg.getBlockUser() ? "Blocked" : "Kicked") + " from ACARS server");
		
		try {
			Connection con = ctx.getConnection();
			ctx.startTX();
			
			// Write the KICK record
			SetStatusUpdate udao = new SetStatusUpdate(con);
			udao.write(upd, ac.getUserData().getDB());
			
			// Get the pilot record
			GetPilot pdao = new GetPilot(con);
			Pilot p = pdao.get(ac.getUserData());
			
			// Restrict ACARS access
			if (p != null) {
				p.setACARSRestriction(msg.getBlockUser() ? Restriction.BLOCK : Restriction.RESTRICT);
				SetPilot pwdao = new SetPilot(con);
				pwdao.write(p, ac.getUserData().getDB());
			} else
				log.warn("Cannot update Pilot record for {}", ac.getUser().getName());
		} catch (DAOException de) {
			ctx.rollbackTX();
			log.atError().withThrowable(de).log("Cannot log KICK - {}", de.getMessage());
		} finally {
			ctx.release();
		}
		
		// Remove the connection
		ac.close();
		ctx.getACARSConnectionPool().remove(ac);
	}
}