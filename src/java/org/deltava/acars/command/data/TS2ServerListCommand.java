// Copyright 2006, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.TS2ServerMessage;

import org.deltava.dao.DAOException;
import org.deltava.dao.GetTS2Data;

/**
 * An ACARS Server data command to display TeamSpeak 2 server data.
 * @author Luke
 * @version 7.0
 * @since 1.0
 */

public class TS2ServerListCommand extends DataCommand {

	/**
	 * Initializes the Command.
	 */
	public TS2ServerListCommand() {
		super(TS2ServerListCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public final void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		TS2ServerMessage rspMsg = new TS2ServerMessage(env.getOwner(), msg.getID());
		try {
			GetTS2Data dao = new GetTS2Data(ctx.getConnection());
			rspMsg.addAll(dao.getServers(env.getOwner().getRoles()));
		} catch (DAOException de) {
			log.error("Error loading TS2 Server datas - " + de.getMessage(), de);
		} finally {
			ctx.release();
		}
		
		ctx.push(rspMsg, env.getConnectionID());
	}
}